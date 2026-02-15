package org.nahap.bookcatalogservice.configuration;

import org.nahap.bookcatalogservice.security.JwtAuthenticationFilter;
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

                        // ========== CORS PREFLIGHT ==========
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ========== ACTUATOR ==========
                        .requestMatchers("/actuator/**").permitAll()

                        // ========== SWAGGER / OPENAPI ==========
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ========== ПУБЛИЧНЫЕ ЭНДПОИНТЫ (Гости) ==========
                        // Авторизация
                        .requestMatchers("/api/auth/login").permitAll()

                        // Регистрация (публичная)
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()

                        // Чтение авторов, книг, жанров, комментариев (только GET)
                        .requestMatchers(HttpMethod.GET, "/api/authors", "/api/authors/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books", "/api/books/**").permitAll()

                        .requestMatchers(HttpMethod.HEAD, "/api/books/*/cover").permitAll()

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
                        .requestMatchers(HttpMethod.POST, "/api/authors").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/authors/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/authors/**").hasAnyRole("MODERATOR", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/genres").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/genres/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/genres/**").hasAnyRole("MODERATOR", "ADMIN")

                        .requestMatchers(HttpMethod.POST, "/api/books").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/books/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/books/**").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/books/upload").hasAnyRole("MODERATOR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/comments/books/*/all").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/comments/*/moderate").hasAnyRole("MODERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/comments/*/restore").hasAnyRole("MODERATOR", "ADMIN")

                        .requestMatchers(HttpMethod.GET, "/api/ratings/books/**").hasAnyRole("MODERATOR", "ADMIN")

                        // ========== СТАТИСТИКА ==========
                        .requestMatchers("/api/statistics/books/top-rated").permitAll()
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

        // Allow all localhost ports (nginx :80, dev :5173, :3000, etc)
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}