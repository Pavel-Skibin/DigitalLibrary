package org.nahap.bookcatalogservice.service;

import lombok.RequiredArgsConstructor;
import org.nahap.bookcatalogservice.entity.GlobalSetting;
import org.nahap.bookcatalogservice.repository.GlobalSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GlobalSettingsService {

    private static final String READING_ENABLED_KEY = "reading_enabled";

    private final GlobalSettingRepository repository;

    public boolean isReadingEnabled() {
        return repository.findById(READING_ENABLED_KEY)
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(false);
    }

    @Transactional
    public boolean toggleReading() {
        boolean newValue = !isReadingEnabled();
        repository.save(new GlobalSetting(READING_ENABLED_KEY, String.valueOf(newValue)));
        return newValue;
    }
}
