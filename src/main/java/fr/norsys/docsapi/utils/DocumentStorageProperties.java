package fr.norsys.docsapi.utils;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Karim
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "file")
public class DocumentStorageProperties {
    private String uploadDir;
}