package org.nahap.userservice.service;

import org.nahap.common.exception.BadRequestException;
import org.nahap.common.exception.ResourceNotFoundException;
import org.nahap.userservice.dto.request.LoginRequest;
import org.nahap.userservice.dto.request.RegisterRequest;
import org.nahap.userservice.dto.response.AuthResponse;
import org.nahap.userservice.dto.response.UserResponse;
import org.nahap.userservice.dto.mapper.UserMapper;
import org.nahap.userservice.entity.User;
import org.nahap.userservice.entity.UserRole;
import org.nahap.userservice.repository.UserRepository;
import org.nahap.userservice.repository.UserRoleRepository;
import org.nahap.userservice.security.CustomUserDetails;
import org.nahap.common.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис аутентификации и регистрации
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       UserRoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Проверка уникальности username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username уже занят: " + request.getUsername());
        }

        // Проверка уникальности email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email уже зарегистрирован: " + request.getEmail());
        }

        // Получаем роль USER
        UserRole userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new ResourceNotFoundException("Роль USER не найдена"));

        // Создаем пользователя
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(userRole);

        user = userRepository.save(user);

        // Генерируем JWT токен
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails, user.getId());

        return new AuthResponse(token, user.getUsername());
    }

    public AuthResponse login(LoginRequest request) {
        // Аутентификация через Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Генерируем JWT токен
        String token = jwtUtil.generateToken(userDetails, userDetails.getId());

        return new AuthResponse(token, userDetails.getUsername());
    }
}
