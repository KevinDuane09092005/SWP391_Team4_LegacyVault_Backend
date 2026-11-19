package com.legacyvault.config;

import com.legacyvault.chatbot.controller.ChatbotAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final ChatbotAuthenticationEntryPoint chatbotAuthenticationEntryPoint;

        @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,https://legacyvault-backend-ue8r.onrender.com,https://legacyvault-chi-lovat.vercel.app}")
        private String allowedOrigins;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter,
                        ChatbotAuthenticationEntryPoint chatbotAuthenticationEntryPoint) {
                this.jwtAuthFilter = jwtAuthFilter;
                this.chatbotAuthenticationEntryPoint = chatbotAuthenticationEntryPoint;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                List<String> originPatterns = Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isEmpty())
                                .toList();

                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(
                                originPatterns.isEmpty() ? List.of("http://localhost:5173") : originPatterns);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setExposedHeaders(List.of("Content-Disposition", "Content-Type", "Content-Length"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(AbstractHttpConfigurer::disable)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/error",
                                                                "/api/v1/demo/face/**",
                                                                "/api/v1/auth/**",
                                                                "/api/v1/contacts/invitation/**",
                                                                "/api/v1/handover/beneficiary/portal-context",
                                                                "/api/v1/handover/beneficiary/request-access-otp",
                                                                "/api/v1/handover/beneficiary/login-with-code",
                                                                "/api/v1/handover/beneficiary/verify-otp",
                                                                "/api/v1/handover/trigger",
                                                                "/api/v1/legal-verification/documents/**")
                                                .permitAll()
                                                .requestMatchers("/api/v1/handover/beneficiary/face/**")
                                                .hasAnyRole("BENEFICIARY", "VAULT_OWNER", "USER")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/plans")
                                                .permitAll()
                                                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SYSTEM_ADMIN")
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                                                chatbotAuthenticationEntryPoint,
                                                new AntPathRequestMatcher("/api/v1/chatbot/**")))
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
                return http.build();
        }
}
