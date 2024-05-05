package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.document.DocumentResponseDto;
import fr.norsys.docsapi.dto.document.ShareDto;
import fr.norsys.docsapi.entity.Document;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

public interface IDocumentService {
    ResponseEntity<String> upload(MultipartFile document, String metadata);
    ResponseEntity<Resource> download(String docId);
    Document get(UUID id);
    ResponseEntity<?> getList();
    ResponseEntity<?> delete(String id);
    ResponseEntity<List<DocumentResponseDto>> search(String searchValue);
    ResponseEntity<?> share(ShareDto shareDto);
    ResponseEntity<List<DocumentResponseDto>> sharedWithMe();
    ResponseEntity<List<DocumentResponseDto>> searchSharedWithMe(String searchValue);
}
