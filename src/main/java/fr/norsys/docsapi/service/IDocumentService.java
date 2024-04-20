package fr.norsys.docsapi.service;

import fr.norsys.docsapi.dto.document.DocumentResponseDto;
import fr.norsys.docsapi.dto.document.ShareDto;
import fr.norsys.docsapi.entity.Document;
import fr.norsys.docsapi.entity.MetaData;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IDocumentService {
    String uploadDocument(MultipartFile document, List<MetaData> metadata) throws IOException, NoSuchAlgorithmException;
    Resource downloadDocument(String docId) throws IOException, NoSuchAlgorithmException;
    Document get(UUID id);
    List<DocumentResponseDto> getList();
    void delete(String id) throws IOException;
    List<DocumentResponseDto> search(String searchValue);
    Map<String, Object> getListPagination(int page, int size);
    void share(ShareDto shareDto);
}
