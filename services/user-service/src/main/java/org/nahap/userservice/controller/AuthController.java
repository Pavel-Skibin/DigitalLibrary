package org.nahap.userservice.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.nahap.userservice.dto.request.LoginRequest;
import org.nahap.userservice.dto.response.AuthResponse;
import org.nahap.userservice.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private static final int JWT_EXPIRATION_SECONDS = 86400; // 24 часа
    
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);

        ResponseCookie cookie = ResponseCookie.from("jwt", authResponse.getToken())
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .maxAge(JWT_EXPIRATION_SECONDS)
                .sameSite("Lax")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie clearCookie = ResponseCookie.from("jwt", "")
                .httpOnly(false)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
        
        return ResponseEntity.ok().build();
    }
}
