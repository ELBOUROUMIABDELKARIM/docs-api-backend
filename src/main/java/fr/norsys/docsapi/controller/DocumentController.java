package fr.norsys.docsapi.controller;


import fr.norsys.docsapi.dto.document.ShareDto;
import fr.norsys.docsapi.service.IDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    public ResponseEntity<String> upload(@RequestPart("document") MultipartFile document, @RequestParam("metadata") String metadata) {
        return documentService.upload(document, metadata);
    }

    @GetMapping(value = "")
    public ResponseEntity<?> getDocuments() {
        return documentService.getList();
    }

    @GetMapping("/download/{docId}")
    public ResponseEntity<Resource> download(@PathVariable String docId) {
        return documentService.download(docId);
    }

    @GetMapping(value = "{docId}")
    public ResponseEntity<?> getDocument(@PathVariable String docId) {
        return ResponseEntity.status(HttpStatus.FOUND).body(documentService.get(UUID.fromString(docId)));
    }


    @GetMapping(value = "/search")
    public ResponseEntity<?> searchDocuments(@RequestParam(defaultValue = "") String searchValue) {
        return documentService.search(searchValue);
    }


    @GetMapping(value = "/searchwithme")
    public ResponseEntity<?> searchWithMeDocument(@RequestParam(defaultValue = "") String searchValue){
        return documentService.searchSharedWithMe(searchValue);
    }

    @PostMapping("/share")
    public ResponseEntity<?> share(@RequestBody ShareDto shareDto){
        return documentService.share(shareDto);
    }


    @DeleteMapping(value = "/{docId}")
    public ResponseEntity<?> delete(@PathVariable String docId) {
        return documentService.delete(docId);
    }


    @GetMapping("/sharedwithme")
    public ResponseEntity<?> sharedWithMe() {
        return documentService.sharedWithMe();
    }

}
