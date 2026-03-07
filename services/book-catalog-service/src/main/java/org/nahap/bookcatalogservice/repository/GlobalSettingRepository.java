package org.nahap.bookcatalogservice.repository;

import org.nahap.bookcatalogservice.entity.GlobalSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlobalSettingRepository extends JpaRepository<GlobalSetting, String> {
}
