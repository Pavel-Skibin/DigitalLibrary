package org.nahap.userservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.nahap.userservice.dto.request.LoginRequest;
import org.nahap.userservice.dto.response.AuthResponse;
import org.nahap.userservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Контроллер для аутентификации
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private static final int JWT_EXPIRATION_SECONDS = 86400; // 24 часа

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        
        // Установить JWT в cookie (как в монолите)
        Cookie cookie = new Cookie("jwt", authResponse.getToken());
        cookie.setHttpOnly(false);  // false чтобы JS мог читать
        cookie.setSecure(false);    // false для http (true для https в production)
        cookie.setPath("/");
        cookie.setMaxAge(JWT_EXPIRATION_SECONDS);
        response.addCookie(cookie);
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Очистить cookie
        Cookie clearCookie = new Cookie("jwt", "");
        clearCookie.setHttpOnly(false);
        clearCookie.setSecure(false);
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);
        
        return ResponseEntity.ok().build();
    }
}
