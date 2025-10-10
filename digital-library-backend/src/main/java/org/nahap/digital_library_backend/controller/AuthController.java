// src/main/java/org/nahap/digital_library_backend/controller/AuthController.java
package org.nahap.digital_library_backend.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nahap.digital_library_backend.dto.request.LoginRequest;
import org.nahap.digital_library_backend.dto.response.AuthResponse;
import org.nahap.digital_library_backend.security.CustomUserDetails;
import org.nahap.digital_library_backend.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    private static final int JWT_EXPIRATION_SECONDS = 86400; // 24 часа. Потом продумаю с refresh токенами. А пока так. Безопасность моё ВСЁ

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {

        log.info("Login attempt for user: {}", loginRequest.username());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtil.generateToken(userDetails);

        Cookie cookie = new Cookie("jwt", jwtToken);
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(JWT_EXPIRATION_SECONDS);
        response.addCookie(cookie);
        log.info("User {} logged in successfully. JWT cookie set.", loginRequest.username());
        return ResponseEntity.ok(new AuthResponse(jwtToken, userDetails.getUsername()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        log.info("Logout request received");

        Cookie clearCookie = new Cookie("jwt", "");
        clearCookie.setHttpOnly(false);
        clearCookie.setSecure(false);
        clearCookie.setPath("/");
        clearCookie.setMaxAge(0);
        response.addCookie(clearCookie);

        log.info("JWT cookie cleared");
        return ResponseEntity.ok().build();
    }
}