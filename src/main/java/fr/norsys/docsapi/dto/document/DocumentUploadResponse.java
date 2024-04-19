package fr.norsys.docsapi.dto.document;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentUploadResponse {
    private String docName;
    private String downloadUri;
}