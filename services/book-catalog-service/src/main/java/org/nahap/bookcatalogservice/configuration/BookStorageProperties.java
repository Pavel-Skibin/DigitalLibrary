package org.nahap.bookcatalogservice.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.books")
@Data
public class BookStorageProperties {
    private String storagePath;


}