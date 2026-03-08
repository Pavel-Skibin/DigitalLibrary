package org.nahap.bookcatalogservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "global_settings")
@Getter
@Setter
@NoArgsConstructor
public class GlobalSetting {

    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "value", length = 500, nullable = false)
    private String value;

    public GlobalSetting(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
