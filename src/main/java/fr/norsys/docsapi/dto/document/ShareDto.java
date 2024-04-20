package fr.norsys.docsapi.dto.document;


import lombok.Data;

import java.util.List;

@Data
public class ShareDto {
    private String documentId;
    private List<String> usersIds;
    private String permissions;
}
