package fr.norsys.docsapi.utils;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
public class LocalFileStorage {

    private final DocumentStorageProperties documentStorageProperties;

    public LocalFileStorage(DocumentStorageProperties documentStorageProperties) {
        this.documentStorageProperties = documentStorageProperties;
    }


    public Path getUploadPath() {
        return Paths.get(documentStorageProperties.getUploadDir());
    }

    public String getOriginalFilename(MultipartFile file) {
        return StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
    }

    public String getContentType(Path filePath) throws IOException {
        String type = Files.probeContentType(filePath);
        return type != null ? type : "unknown";
    }

    public void createUploadDirectories(Path uploadPath) {
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create upload directories", e);
        }
    }

    public void handleUploadFailure(Path filePath, Exception e) {
        try {
            Files.delete(filePath);
        } catch (IOException io) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete uploaded file", io);
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to process document upload", e);
    }

}
