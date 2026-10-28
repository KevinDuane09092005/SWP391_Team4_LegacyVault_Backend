package com.legacyvault.config;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import com.legacyvault.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (jwtService.validateToken(token)) {
            String userId = jwtService.extractUserId(token);
            userRepository.findById(userId)
                    .filter(this::isActive)
                    .ifPresent(user -> authenticateRequest(request, user));
        }

        filterChain.doFilter(request, response);
    }

    private boolean isActive(User user) {
        return user.getAccountStatus() == null || !"SUSPENDED".equalsIgnoreCase(user.getAccountStatus());
    }

    private void authenticateRequest(HttpServletRequest request, User user) {
        String role = user.getRole();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (role != null && !role.isBlank()) {
            String upper = role.trim().toUpperCase(Locale.ROOT);
            authorities.add(new SimpleGrantedAuthority("ROLE_" + upper));
            if ("ADMIN".equals(upper)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SYSTEM_ADMIN"));
            } else if ("SYSTEM_ADMIN".equals(upper)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user.getId(), null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
