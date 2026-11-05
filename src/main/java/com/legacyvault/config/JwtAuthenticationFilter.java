package com.legacyvault.config;

import com.legacyvault.entity.Contact;
import com.legacyvault.entity.User;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.UserRepository;
import com.legacyvault.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final ContactRepository contactRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this(jwtService, userRepository, null);
    }

    @Autowired
    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository,
            ContactRepository contactRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.contactRepository = contactRepository;
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
                    .ifPresentOrElse(
                            user -> {
                                if (isActive(user)) {
                                    String effectiveRole = (user.getRole() != null && !user.getRole().isBlank())
                                            ? user.getRole()
                                            : jwtService.extractRole(token);
                                    List<String> extraRoles = resolveDesignatedRoles(user.getEmail());
                                    authenticateRequest(request, user.getId(), effectiveRole, extraRoles);
                                }
                            },
                            () -> {
                                String tokenRole = jwtService.extractRole(token);
                                if ("BENEFICIARY".equalsIgnoreCase(tokenRole)) {
                                    authenticateRequest(request, userId, "BENEFICIARY", List.of());
                                }
                            }
                    );
        }

        filterChain.doFilter(request, response);
    }

    private List<String> resolveDesignatedRoles(String email) {
        List<String> extraRoles = new ArrayList<>();
        if (contactRepository == null || email == null || email.isBlank()) {
            return extraRoles;
        }
        try {
            List<Contact> contacts = contactRepository.findByEmail(email.trim());
            if (contacts != null) {
                boolean isBeneficiary = contacts.stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsBeneficiary()));
                boolean isExecutor = contacts.stream().anyMatch(c -> Boolean.TRUE.equals(c.getIsExecutor()));
                if (isBeneficiary) {
                    extraRoles.add("BENEFICIARY");
                }
                if (isExecutor) {
                    extraRoles.add("EXECUTOR");
                    extraRoles.add("DIGITAL_EXECUTOR");
                }
            }
        } catch (Exception ignored) {
            // Do not fail the request if contact lookup fails
        }
        return extraRoles;
    }

    private boolean isActive(User user) {
        String status = user.getAccountStatus();
        return status == null
                || (!"SUSPENDED".equalsIgnoreCase(status) && !"DEACTIVATED".equalsIgnoreCase(status));
    }

    private void authenticateRequest(HttpServletRequest request, String principalId, String role, List<String> extraRoles) {
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
        if (extraRoles != null) {
            for (String extra : extraRoles) {
                if (extra != null && !extra.isBlank()) {
                    String upper = extra.trim().toUpperCase(Locale.ROOT);
                    SimpleGrantedAuthority auth = new SimpleGrantedAuthority("ROLE_" + upper);
                    if (!authorities.contains(auth)) {
                        authorities.add(auth);
                    }
                }
            }
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principalId, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
