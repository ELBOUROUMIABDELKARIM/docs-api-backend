package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.document.DocumentResponseDto;
import fr.norsys.docsapi.entity.*;
import fr.norsys.docsapi.repository.DocumentRepository;
import fr.norsys.docsapi.repository.MetaDataRepository;
import fr.norsys.docsapi.repository.PermissionEntryRepository;
import fr.norsys.docsapi.repository.UserRepository;
import fr.norsys.docsapi.security.service.UserDetailsImpl;
import fr.norsys.docsapi.specifications.DocumentSpecifications;
import fr.norsys.docsapi.utils.DocumentHashCalculator;
import fr.norsys.docsapi.utils.DocumentStorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DocumentLocalService implements IDocumentService {

    private final DocumentStorageProperties documentStorageProperties;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final MetaDataRepository metaDataRepository;
    private final PermissionEntryRepository permissionEntryRepository;

    public DocumentLocalService(DocumentStorageProperties documentStorageProperties, DocumentRepository documentRepository, UserRepository userRepository, MetaDataRepository metaDataRepository, PermissionEntryRepository permissionEntryRepository) {
        this.documentStorageProperties = documentStorageProperties;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.metaDataRepository = metaDataRepository;
        this.permissionEntryRepository = permissionEntryRepository;
    }

    @Override
    public String uploadDocument(MultipartFile file, List<MetaData> metadata) throws IOException, NoSuchAlgorithmException {
        Path uploadPath = Paths.get(documentStorageProperties.getUploadDir());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        User user = userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            String checksum = DocumentHashCalculator.calculateHash(filePath);
            validateDocument(checksum, originalFilename);
            String type = Files.probeContentType(filePath);
            if (type == null) {
                type = "unknown";
            }
            Document document = Document.builder()
                    .name(originalFilename)
                    .size(file.getSize())
                    .type(type)
                    .creationDate(Timestamp.from(Instant.now()))
                    .modificationDate(Timestamp.from(Instant.now()))
                    .checksum(checksum)
                    .storageLocation(filePath.toString())
                    .user(user)
                    .build();
            documentRepository.save(document);
            PermissionEntry permissionEntry = PermissionEntry.builder()
                    .document(document)
                    .user(user)
                    .permission(Permission.ALL)
                    .build();
            permissionEntryRepository.save(permissionEntry);
            for (MetaData metaDataItem : metadata) {
                metaDataItem.setDocument(document);
                metaDataRepository.save(metaDataItem);
            }
            return String.valueOf(document.getId());
        } catch (IOException e) {
            Files.delete(filePath);
            throw new IOException("Could not save document: " + originalFilename, e);
        }
    }


    @Override
    public Resource downloadDocument(String docId) throws IOException {
        Document document = documentRepository.findById(UUID.fromString(docId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        Path dirPath = Paths.get(documentStorageProperties.getUploadDir());
        Path foundFile = Files.list(dirPath)
                .filter(file -> file.getFileName().toString().startsWith(document.getName()))
                .findFirst()
                .orElseThrow(() -> new IOException("Document not found"));

        return new UrlResource(foundFile.toUri());
    }

    @Override
    public Document get(String id) {
        return null;
    }

    @Override
    public List<DocumentResponseDto> getList() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userDetails = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow();
            List<Document> documents = documentRepository.findByUser(user);
            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }
    @Override
    public Map<String, Object> getListPagination(int page, int size) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userDetails = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow();
            Pageable paging = PageRequest.of(page, size);
            Page<Document> pageDocs;
            pageDocs = documentRepository.findByUser(user, paging);
            List<DocumentResponseDto> documents = pageDocs.getContent().stream()
                    .map(this::convertToDto)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents);
            response.put("currentPage", pageDocs.getNumber());
            response.put("totalItems", pageDocs.getTotalElements());
            response.put("totalPages", pageDocs.getTotalPages());

            return response;
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }


    @Override
    public void delete(String id) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<DocumentResponseDto> search(String searchValue) {
        try {
            Specification<Document> specification = DocumentSpecifications.search(searchValue);
            List<Document> documents = documentRepository.findAll(specification);
            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }

    private void createDirectories(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    private void validateDocument(String checksum, String filename) {
        if (documentRepository.findByChecksum(checksum).isPresent() || documentRepository.findByName(filename).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document already exists");
        }
    }

    public DocumentResponseDto convertToDto(Document document) {
        return DocumentResponseDto.builder()
                .id(document.getId())
                .name(document.getName())
                .type(document.getType())
                .size(document.getSize())
                .owner(document.getUser().getUserName())
                .dateCreation(String.valueOf(document.getCreationDate()))
                .dateModification(String.valueOf(document.getModificationDate()))
                .permissions(document.getPermissions())
                .metaData(document.getMetadata())
                .build();
    }

}
