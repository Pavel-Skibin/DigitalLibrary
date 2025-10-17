package org.nahap.digital_library_backend.configuration;// src/main/java/org/nahap/digital_library_backend/config/SecurityConfig.java


import org.nahap.digital_library_backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, UserDetailsService userDetailsService) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
    }





    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // ========== ПУБЛИЧНЫЕ ЭНДПОИНТЫ (Гости) ==========
                        // Авторизация
                        .requestMatchers("/api/auth/login").permitAll()

                        // Регистрация (публичная)
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/books/*/cover").permitAll()

                        // Чтение авторов, книг, жанров, комментариев (только GET)
                        .requestMatchers(HttpMethod.GET, "/api/authors", "/api/authors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/genres", "/api/genres/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/comments/books/*").permitAll()
                        .requestMatchers("/api/books/search", "/api/books/*/fb2", "/api/books/*/download").permitAll()

                        // ========== USER (авторизованные пользователи) ==========
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/bookmarks/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/comments").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/comments/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*").authenticated()
                        .requestMatchers("/api/ratings/**").authenticated()

                        // ========== MODERATOR + ADMIN ==========
                        // Управление авторами
                        .requestMatchers(HttpMethod.POST, "/api/authors").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/authors/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/authors/**").hasAnyRole("MODERATOR", "ADMIN")

                        // Управление жанрами
                        .requestMatchers(HttpMethod.POST, "/api/genres").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/genres/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/genres/**").hasAnyRole("MODERATOR", "ADMIN")

                        // Управление книгами
                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/books/upload").hasAnyRole("MODERATOR", "ADMIN")

                        // Модерация комментариев
                        .requestMatchers(HttpMethod.GET, "/api/comments/books/*/all").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*/moderate").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/comments/*/restore").hasAnyRole("MODERATOR", "ADMIN")

                        // Просмотр оценок книги
                        .requestMatchers(HttpMethod.GET, "/api/ratings/books/**").hasAnyRole("MODERATOR", "ADMIN")

                        // ========== СТАТИСТИКА (MODERATOR + ADMIN) ==========
                        .requestMatchers("/api/statistics/system").permitAll()
                        .requestMatchers("/api/statistics/**").hasAnyRole("MODERATOR", "ADMIN")

                        // ========== ТОЛЬКО ADMIN ==========
                        .requestMatchers("/api/users/**").hasRole("ADMIN")

                        // Любые другие запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();


        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}