package com.connecthub.auth.config;

import com.connecthub.auth.oauth2.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor  // OAuth2SuccessHandler inject karne ke liye
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    // ─────────────────────────────────────────────────────────────────────────
    // Public Endpoints (same as before — kuch nahi hataya)
    // ─────────────────────────────────────────────────────────────────────────

    private static final String[] PUBLIC_PATHS = {

            // Auth APIs
            "/auth/register",
            "/auth/login",
            "/auth/refresh",
            "/auth/validate",

            // ✅ OAuth2 endpoints (Google login flow)
            "/oauth2/**",
            "/login/oauth2/**",

            // Swagger / OpenAPI
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/v3/api-docs",
            "/swagger-resources/**",
            "/webjars/**"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Security Filter Chain
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // Disable CSRF
            .csrf(AbstractHttpConfigurer::disable)

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Session policy — OAuth2 ke liye STATELESS nahi chal sakta during redirect,
            // isliye IF_REQUIRED use karo (JWT-based APIs ke liye same behavior hai)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            // Authorization — same as before
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().permitAll()
            )

            // ✅ Google OAuth2 Login — sirf ye block naya hai
            .oauth2Login(oauth -> oauth
                .authorizationEndpoint(ep ->
                    ep.baseUri("/auth/oauth2/authorize"))   // frontend ka googleAuthUrl yahi hit karta hai
                .redirectionEndpoint(ep ->
                    ep.baseUri("/login/oauth2/callback/*")) // Google Console mein ye URI add karo
                .successHandler(oAuth2SuccessHandler)
            );

        return http.build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CORS Configuration (same as before)
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*"));

        config.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "PATCH",
                "OPTIONS"
        ));

        config.setAllowedHeaders(List.of("*"));

        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Password Encoder (same as before)
    // ─────────────────────────────────────────────────────────────────────────

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
