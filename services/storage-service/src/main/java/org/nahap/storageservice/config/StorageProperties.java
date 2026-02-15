package org.nahap.storageservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private String booksPath = "/app/books";
    private String coversPath = "/app/covers";
    private long maxFileSize = 52428800; // 50MB
}
