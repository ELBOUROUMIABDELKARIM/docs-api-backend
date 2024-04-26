package fr.norsys.docsapi.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.norsys.docsapi.dto.document.DocumentResponseDto;
import fr.norsys.docsapi.dto.document.DocumentUploadResponse;
import fr.norsys.docsapi.dto.document.ShareDto;
import fr.norsys.docsapi.entity.MetaData;
import fr.norsys.docsapi.service.IDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(exposedHeaders = {"Content-Disposition"})
public class DocumentController {

    private final IDocumentService documentService;

    public DocumentController(IDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(value = "/upload")
    public ResponseEntity<?> upload(@RequestPart("document") MultipartFile document, @RequestParam("metadata") String metadata) {
        try {
            List<MetaData> metadataMapped = new ObjectMapper().readValue(metadata, new TypeReference<>() {});
            documentService.upload(document, metadataMapped);
            return ResponseEntity.status(HttpStatus.CREATED).body(document.getOriginalFilename()+" Saved Successfully");
        }catch (JsonProcessingException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid metadata");
        }
        catch (IOException | NoSuchAlgorithmException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getCause());
        }
    }

    @GetMapping(value = "")
    public ResponseEntity<?> getDocuments() {
        try {
            return ResponseEntity.status(HttpStatus.FOUND).body(documentService.getList());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving documents");
        }
    }

    @GetMapping(value = "{docId}")
    public ResponseEntity<?> getDocument(@PathVariable String docId) {
        return ResponseEntity.status(HttpStatus.FOUND).body(documentService.get(UUID.fromString(docId)));
    }


    @GetMapping(value = "/page")
    public ResponseEntity<?> getDocumentsPagination(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        try {
            return ResponseEntity.status(HttpStatus.FOUND).body(documentService.getListPagination(page, size));
        } catch (Exception e) {
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/download/{docId}")
    public ResponseEntity<Resource> download(@PathVariable String docId)
            throws IOException, NoSuchAlgorithmException {
        Resource resource = documentService.download(docId);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String headerValue = "attachment; filename=\"" + resource.getFilename() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, headerValue)
                .body(resource);
    }


    @GetMapping(value = "/search")
    public ResponseEntity<?> searchDocuments(@RequestParam(defaultValue = "") String searchValue) {
        try {
            List<DocumentResponseDto> documents = documentService.search(searchValue);
            return ResponseEntity.status(HttpStatus.FOUND).body(documents);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error Searching documents");
        }
    }


    @GetMapping(value = "/searchwithme")
    public ResponseEntity<?> searchWithMeDocument(@RequestParam(defaultValue = "") String searchValue){
        try {
            return ResponseEntity.status(HttpStatus.FOUND).body(documentService.searchSharedWithMe(searchValue));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error Searching documents");
        }
    }


    @DeleteMapping(value = "/{docId}")
    public ResponseEntity<?> delete(@PathVariable String docId) {
        try {
            documentService.delete(docId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        }
    }


    @PostMapping("/share")
    public ResponseEntity<?> share(@RequestBody ShareDto shareDto){
        try {
            documentService.share(shareDto);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("File Successfully Shared");
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    @GetMapping("/sharedwithme")
    public ResponseEntity<?> sharedWithMe() {
        try {
            return ResponseEntity.status(HttpStatus.FOUND).body(documentService.sharedWithMe());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving Shared documents");
        }
    }
}
