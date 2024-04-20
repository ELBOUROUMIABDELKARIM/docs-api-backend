package fr.norsys.docsapi.dto.document;

import fr.norsys.docsapi.entity.MetaData;
import fr.norsys.docsapi.entity.PermissionEntry;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DocumentResponseDto {
    private UUID id;
    private String name;
    private String type;
    private long size;
    private String owner;
    private String dateCreation;
    private String dateModification;
    private List<PermissionEntry> permissions;
    private List<MetaData> metaData;
}
