package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.document.DocumentResponseDto;
import fr.norsys.docsapi.dto.document.ShareDto;
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
import java.nio.file.*;
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

    /**
     * Karim
     * */
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

    /**
     * Karim
     * */
    @Override
    public Resource downloadDocument(String docId) throws IOException {
        Document document = get(UUID.fromString(docId));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!document.getUser().equals(user)) {
            PermissionEntry permissionEntry = permissionEntryRepository.findByDocumentAndUser(document, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission entry not found for this user and document"));
            if (!permissionEntry.getPermission().equals(Permission.ALL) && !permissionEntry.getPermission().equals(Permission.READ)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to download this document");
            }
        }
        try {
            Path foundFile = Paths.get(documentStorageProperties.getUploadDir(), document.getName());
            if (!Files.exists(foundFile)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            return new UrlResource(foundFile.toUri());
        } catch (InvalidPathException e) {
            throw new IOException("Document not found");
        }
    }

    /**
     * Karim
     * */
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

    /**
     * Aymane
     * */
    @Override
    public Map<String, Object> getListPagination(int page, int size) {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userDetails = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow();
            Pageable paging = PageRequest.of(page, size);
            Page<Document> pageDocs =  documentRepository.findByUser(user, paging);
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

    /**
     * Karim
     * */
    @Override
    public void share(ShareDto shareDto) {
        Document document = get(UUID.fromString(shareDto.getDocumentId()));
        List<Permission> permissions = Arrays.stream(shareDto.getPermissions().split(","))
                .map(Permission::valueOf)
                .toList();

        List<Permission> clearPermissions = clearPermissions(permissions);

        shareDto.getUsersIds().forEach(userId -> {
            User user = userRepository.findById(UUID.fromString(userId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                clearPermissions.forEach(permission -> {
                if(!isDocumentSharedWithUser(document, user, permission)){
                    PermissionEntry permissionEntry = new PermissionEntry();
                    permissionEntry.setDocument(document);
                    permissionEntry.setUser(user);
                    permissionEntry.setPermission(permission);
                    permissionEntryRepository.save(permissionEntry);
                }
            });
        });
    }

    /**
     * Karim
     * */
    @Override
    public List<DocumentResponseDto> sharedWithMe() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userDetails = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow();
            List<Document> documents = documentRepository.findSharedDocumentsForUser(user)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No Shared Documents found"));
            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }

    }

    /**
     * Karim
     * */
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

    /**
     * Aymane
     * */
    @Override
    public Document get(UUID id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

    }

    /**
     * Aymane
     * */
    @Override
    public void delete(String id) throws IOException {
        Document document = get(UUID.fromString(id));
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userDetails = (UserDetailsImpl) authentication.getPrincipal();
        var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!document.getUser().equals(user)) {
            PermissionEntry permissionEntry = permissionEntryRepository.findByDocumentAndUser(document, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission entry not found for this user and document"));
            if (!permissionEntry.getPermission().equals(Permission.ALL) && !permissionEntry.getPermission().equals(Permission.DELETE)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to delete this document");
            }
        }
        try {
            Path foundFile = Paths.get(documentStorageProperties.getUploadDir(), document.getName());
            if (!Files.exists(foundFile)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
            }
            Files.delete(foundFile);
            documentRepository.delete(document);
        } catch (IOException e) {
            throw new IOException("Failed to delete document");
        }
    }

    /**
     * Karim
     * */
    public void createDirectories(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }

    /**
     * Karim
     * */
    public void validateDocument(String checksum, String filename) {
        if (documentRepository.findByChecksum(checksum).isPresent() || documentRepository.findByName(filename).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document already exists");
        }
    }

    /**
     * Karim
     * */
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

    /**
     * Karim
     * */
    private List<Permission> clearPermissions(List<Permission> permissions) {
        if (permissions.contains(Permission.ALL)) {
            return Collections.singletonList(Permission.ALL);
        } else if (new HashSet<>(permissions).containsAll(Arrays.asList(Permission.READ, Permission.WRITE, Permission.DELETE))) {
            return Collections.singletonList(Permission.ALL);
        } else if (permissions.contains(Permission.READ) && permissions.contains(Permission.WRITE)) {
            return Arrays.asList(Permission.READ, Permission.WRITE);
        } else if (permissions.contains(Permission.READ) && permissions.contains(Permission.DELETE)) {
            return Arrays.asList(Permission.READ, Permission.DELETE);
        } else if (permissions.contains(Permission.WRITE) && permissions.contains(Permission.DELETE)) {
            return Arrays.asList(Permission.WRITE, Permission.DELETE);
        } else {
            return permissions;
        }
    }


    /**
     * Karim
     * */
    private boolean isDocumentSharedWithUser(Document document, User user, Permission permission) {
        return document.getPermissions().stream()
                .anyMatch(entry -> entry.getUser().equals(user) && entry.getPermission() == permission);
    }
}
