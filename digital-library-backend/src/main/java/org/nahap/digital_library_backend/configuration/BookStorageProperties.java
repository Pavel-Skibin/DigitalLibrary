package org.nahap.digital_library_backend.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.books")
@Data
public class BookStorageProperties {
    private String storagePath;


}