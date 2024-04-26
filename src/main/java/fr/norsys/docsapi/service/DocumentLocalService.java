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
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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


    @Override
    public void upload(MultipartFile file, List<MetaData> metadata) throws IOException, NoSuchAlgorithmException {
        User user = getAuthenticatedUser();
        Path uploadPath = Paths.get(documentStorageProperties.getUploadDir());
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
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
            Document document = createDocument(originalFilename, file.getSize(), type,checksum, filePath.toString(), user);
            documentRepository.save(document);

            PermissionEntry permissionEntry = PermissionEntry.builder()
                    .document(document)
                    .user(user)
                    .permission(Permission.ALL)
                    .build();
            permissionEntryRepository.save(permissionEntry);

            metadata.forEach(metaDataItem -> {
                metaDataItem.setDocument(document);
                metaDataRepository.save(metaDataItem);
            });

        } catch (IOException e) {
            Files.delete(filePath);
            throw new IOException("Could not save document: " + originalFilename, e);
        }
    }

    @Override
    public Resource download(String docId) throws IOException {
        Document document = get(UUID.fromString(docId));
        User user = getAuthenticatedUser();

        if (!document.getUser().equals(user)) {
            boolean hasReadPermission = hasPermission(document, user, Permission.READ);
            if (!hasReadPermission) {
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

    @Override
    public List<DocumentResponseDto> getList() {
        try {
            User user = getAuthenticatedUser();
            List<Document> documents = documentRepository.findByUser(user)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "No documents found"));
            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please try again later.", e);
        }
    }

    @Override
    public Map<String, Object> getListPagination(int page, int size) {
        try {
            User user = getAuthenticatedUser();

            Pageable paging = PageRequest.of(page, size);
            Page<Document> pageDocs = documentRepository.findByUser(user, paging);
            List<DocumentResponseDto> documents = pageDocs.getContent().stream()
                    .map(this::convertToDto)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("documents", documents);
            response.put("currentPage", pageDocs.getNumber());
            response.put("totalItems", pageDocs.getTotalElements());
            response.put("totalPages", pageDocs.getTotalPages());
            return response;

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please try again later.", e);
        }
    }

    @Override
    public void share(ShareDto shareDto) {
        try{
            Document document = get(UUID.fromString(shareDto.getDocumentId()));
            List<Permission> requestedPermissions = parsePermissions(shareDto.getPermissions());
            validatePermissions(requestedPermissions);
            List<Permission> effectivePermissions = resolveEffectivePermissions(requestedPermissions);

            List<String> userIds = shareDto.getUsersIds().stream()
                    .filter(userId -> !userId.equals(document.getUser().getId().toString()))
                    .toList();

            userIds.forEach(userId -> {
                User user = userRepository.findById(UUID.fromString(userId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

                permissionEntryRepository.deleteByDocumentAndUser(document, user);

                effectivePermissions.forEach(permission -> {
                    PermissionEntry permissionEntry = new PermissionEntry();
                    permissionEntry.setDocument(document);
                    permissionEntry.setUser(user);
                    permissionEntry.setPermission(permission);
                    permissionEntryRepository.save(permissionEntry);
                });
            });
        }catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

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

    @Override
    public List<DocumentResponseDto> search(String searchValue) {
        try {
            User user = getAuthenticatedUser();
            Specification<Document> specification = Specification.where(DocumentSpecifications.search(searchValue))
                    .and((root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("user"), user));
            List<Document> documents = documentRepository.findAll(specification);
            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }

    @Override
    public List<DocumentResponseDto> searchSharedWithMe(String searchValue) {
        try {
            User user = getAuthenticatedUser();

            Specification<Document> sharedWithMeSpec = (root, query, criteriaBuilder) -> {
                Join<Document, PermissionEntry> permissionJoin = root.join("permissions", JoinType.INNER);
                return criteriaBuilder.equal(permissionJoin.get("user"), user);
            };

            Specification<Document> notOwnedByUserSpec = (root, query, criteriaBuilder) ->
                    criteriaBuilder.notEqual(root.get("user"), user);

            Specification<Document> combinedSpec = Specification.where(DocumentSpecifications.search(searchValue))
                    .and(sharedWithMeSpec)
                    .and(notOwnedByUserSpec);

            List<Document> documents = documentRepository.findAll(combinedSpec);

            return documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }

    @Override
    public Document get(UUID id) {
        User authenticatedUser = getAuthenticatedUser();
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));

        if (!document.getUser().equals(authenticatedUser) &&
                document.getPermissions().stream().noneMatch(permissionEntry -> permissionEntry.getUser().equals(authenticatedUser))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User does not have permission to access this document");
        }

        return document;
    }

    @Override
    public void delete(String id) {
        Document document = get(UUID.fromString(id));
        User user = getAuthenticatedUser();

        if (!document.getUser().equals(user)) {
            boolean hasDeletePermission = hasPermission(document, user, Permission.DELETE);
            if (!hasDeletePermission) {
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
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete document", e);
        }
    }

    // Helpers

    private void validatePermissions(List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (!Arrays.asList(Permission.values()).contains(permission)) {
                throw new IllegalArgumentException("Invalid permission: " + permission);
            }
        }
    }

    private List<Permission> resolveEffectivePermissions(List<Permission> requestedPermissions) {
        if (requestedPermissions.contains(Permission.ALL) ||
                new HashSet<>(requestedPermissions).containsAll(Arrays.asList(Permission.READ, Permission.WRITE, Permission.DELETE))) {
            return Collections.singletonList(Permission.ALL);
        } else if (requestedPermissions.contains(Permission.READ) && requestedPermissions.contains(Permission.WRITE)) {
            return Arrays.asList(Permission.READ, Permission.WRITE);
        } else if (requestedPermissions.contains(Permission.READ) && requestedPermissions.contains(Permission.DELETE)) {
            return Arrays.asList(Permission.READ, Permission.DELETE);
        } else if (requestedPermissions.contains(Permission.WRITE) && requestedPermissions.contains(Permission.DELETE)) {
            return Arrays.asList(Permission.WRITE, Permission.DELETE);
        } else {
            return requestedPermissions;
        }
    }

    private List<Permission> parsePermissions(String permissionsString) {
        return Arrays.stream(permissionsString.split(","))
                .map(Permission::valueOf)
                .collect(Collectors.toList());
    }

    private User getAuthenticatedUser(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static Document createDocument(String name, long size, String type, String checksum, String storageLocation, User user) {
        Timestamp now = Timestamp.from(Instant.now());
        return Document.builder()
                .name(name)
                .size(size)
                .type(type)
                .creationDate(now)
                .modificationDate(now)
                .checksum(checksum)
                .storageLocation(storageLocation)
                .user(user)
                .build();
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

    private DocumentResponseDto convertToDto(Document document) {
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

    private boolean hasPermission(Document document, User user, Permission requiredPermission) {
        return document.getPermissions().stream()
                .filter(permissionEntry -> permissionEntry.getUser().equals(user))
                .anyMatch(permissionEntry -> permissionEntry.getPermission().equals(requiredPermission) || permissionEntry.getPermission().equals(Permission.ALL));
    }
}
