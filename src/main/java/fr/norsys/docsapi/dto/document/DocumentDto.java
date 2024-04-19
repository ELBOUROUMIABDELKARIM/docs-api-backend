package fr.norsys.docsapi.dto.document;

import fr.norsys.docsapi.entity.MetaData;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class DocumentDto {
    private MultipartFile document;
    private List<MetaData> metadata;
}
