package fr.norsys.docsapi.dto.document;


import lombok.Data;

@Data
public class ShareDto {
    private String documentId;
    private String userId;
    private String permissions;
}
