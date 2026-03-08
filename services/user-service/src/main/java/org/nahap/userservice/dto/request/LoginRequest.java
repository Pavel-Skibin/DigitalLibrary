package org.nahap.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username обязателен")
    private String username;

    @NotBlank(message = "Пароль обязателен")
    private String password;
}
