package fr.norsys.docsapi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import fr.norsys.docsapi.utils.LocalFileStorage;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
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
    private final LocalFileStorage localFileStorage;

    public DocumentLocalService(DocumentStorageProperties documentStorageProperties, DocumentRepository documentRepository, UserRepository userRepository, MetaDataRepository metaDataRepository, PermissionEntryRepository permissionEntryRepository, LocalFileStorage localFileStorage) {
        this.documentStorageProperties = documentStorageProperties;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.metaDataRepository = metaDataRepository;
        this.permissionEntryRepository = permissionEntryRepository;
        this.localFileStorage = localFileStorage;
    }

    @Override
    public ResponseEntity<String> upload(MultipartFile file, String metadata) {
        List<MetaData> metadataMapped = mapMetadata(metadata);
        User user = getAuthenticatedUser();
        Path uploadPath = localFileStorage.getUploadPath();
        String originalFilename = localFileStorage.getOriginalFilename(file);
        localFileStorage.createUploadDirectories(uploadPath);
        Path filePath = uploadPath.resolve(originalFilename);

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String checksum = DocumentHashCalculator.calculateChecksum(filePath);
            validateDocument(checksum, originalFilename);
            String type = localFileStorage.getContentType(filePath);
            Document document = saveDocument(originalFilename, file.getSize(), type, checksum, filePath.toString(), user);
            savePermissionEntry(document, user);
            saveMetadata(metadataMapped, document);
            return ResponseEntity.status(HttpStatus.CREATED).body("Document '" + originalFilename + "' uploaded successfully");
        } catch (IOException | NoSuchAlgorithmException e) {
            localFileStorage.handleUploadFailure(filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong");
        }
    }

    @Override
    public ResponseEntity<Resource> download(String docId) {
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
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
            }
            Resource resource = new UrlResource(foundFile.toUri());
            String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
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
    public ResponseEntity<?> getList() {
        try {
            User user = getAuthenticatedUser();
            List<Document> documents = documentRepository.findByUser(user)
                    .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "No documents found"));
            var docs =  documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.FOUND).body(docs);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please try again later.", e);
        }
    }

    @Override
    public ResponseEntity<?> delete(String id) {
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
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete document", e);
        }
    }

    @Override
    public ResponseEntity<List<DocumentResponseDto>> search(String searchValue) {
        try {
            User user = getAuthenticatedUser();
            Specification<Document> specification = Specification.where(DocumentSpecifications.search(searchValue))
                    .and((root, query, criteriaBuilder) ->
                            criteriaBuilder.equal(root.get("user"), user));
            return executeSearch(specification);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }

    @Override
    public ResponseEntity<?>  share(ShareDto shareDto) {
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

            return ResponseEntity.status(HttpStatus.ACCEPTED).body("File Successfully Shared");
        }catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<List<DocumentResponseDto>> sharedWithMe() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            var userDetails = (UserDetailsImpl) authentication.getPrincipal();
            var user = userRepository.findByUserName(userDetails.getUsername()).orElseThrow();
            List<Document> documents = documentRepository.findSharedDocumentsForUser(user)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No Shared Documents found"));
            var docs = documents.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.FOUND).body(docs);

        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }

    }

    @Override
    public ResponseEntity<List<DocumentResponseDto>> searchSharedWithMe(String searchValue) {
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

            return executeSearch(combinedSpec);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Try Later Please");
        }
    }

    // Helpers

    public List<MetaData> mapMetadata(String metadata) {
        try {
            return new ObjectMapper().readValue(metadata, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid metadata format");
        }
    }

    public ResponseEntity<List<DocumentResponseDto>> executeSearch(Specification<Document> specification) {
        List<Document> documents = documentRepository.findAll(specification);
        if (documents.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No documents found");
        }
        var docs = documents.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.FOUND).body(docs);
    }

    public Document saveDocument(String originalFilename, long size, String type, String checksum, String filePath, User user) {
        Document document = createDocument(originalFilename, size, type, checksum, filePath, user);
        return documentRepository.save(document);
    }

    public void savePermissionEntry(Document document, User user) {
        PermissionEntry permissionEntry = PermissionEntry.builder()
                .document(document)
                .user(user)
                .permission(Permission.ALL)
                .build();
        permissionEntryRepository.save(permissionEntry);
    }

    public void saveMetadata(List<MetaData> metadataMapped, Document document) {
        metadataMapped.forEach(metaDataItem -> {
            metaDataItem.setDocument(document);
            metaDataRepository.save(metaDataItem);
        });
    }

    public void validatePermissions(List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (!Arrays.asList(Permission.values()).contains(permission)) {
                throw new IllegalArgumentException("Invalid permission: " + permission);
            }
        }
    }

    public List<Permission> resolveEffectivePermissions(List<Permission> requestedPermissions) {
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

    public List<Permission> parsePermissions(String permissionsString) {
        return Arrays.stream(permissionsString.split(","))
                .map(Permission::valueOf)
                .collect(Collectors.toList());
    }

    public User getAuthenticatedUser(){
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findByUserName(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public static Document createDocument(String name, long size, String type, String checksum, String storageLocation, User user) {
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

    public void validateDocument(String checksum, String filename) {
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

    public boolean hasPermission(Document document, User user, Permission requiredPermission) {
        return document.getPermissions().stream()
                .filter(permissionEntry -> permissionEntry.getUser().equals(user))
                .anyMatch(permissionEntry -> permissionEntry.getPermission().equals(requiredPermission) || permissionEntry.getPermission().equals(Permission.ALL));
    }

}
