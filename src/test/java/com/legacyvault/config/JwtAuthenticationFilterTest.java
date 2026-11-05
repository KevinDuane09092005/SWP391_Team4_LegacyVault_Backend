package com.legacyvault.config;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import com.legacyvault.service.JwtService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesUsingTheCurrentRoleStoredInTheDatabase() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        User user = user("user-1", "digital_executor", "ACTIVE");
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_DIGITAL_EXECUTOR")));
        verify(jwtService, never()).extractRole(anyString());
    }

    @Test
    void suspendedUserTokenDoesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "admin", "SUSPENDED")));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void deactivatedUserTokenDoesNotAuthenticate() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user("user-1", "vault_owner", "DEACTIVATED")));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void beneficiaryTokenAuthenticatesWhenSubjectIsAContactNotInUserRepository() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);
        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("contact-uuid-1");
        when(userRepository.findById("contact-uuid-1")).thenReturn(Optional.empty());
        when(jwtService.extractRole("valid-token")).thenReturn("BENEFICIARY");

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("contact-uuid-1", authentication.getName());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_BENEFICIARY")));
    }

    @Test
    void userWithDesignatedContactRolesReceivesBothPrimaryAndExtraRoles() throws Exception {
        com.legacyvault.repository.ContactRepository contactRepository = mock(com.legacyvault.repository.ContactRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository, contactRepository);

        User user = user("user-3", "vault_owner", "ACTIVE");
        user.setEmail("sinhleed12@gmail.com");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractUserId("valid-token")).thenReturn("user-3");
        when(userRepository.findById("user-3")).thenReturn(Optional.of(user));

        com.legacyvault.entity.Contact contact = new com.legacyvault.entity.Contact();
        contact.setEmail("sinhleed12@gmail.com");
        contact.setIsBeneficiary(true);
        contact.setIsExecutor(false);
        when(contactRepository.findByEmail("sinhleed12@gmail.com")).thenReturn(java.util.List.of(contact));

        filter.doFilter(requestWithToken(), new MockHttpServletResponse(), mock(FilterChain.class));

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("user-3", authentication.getName());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_VAULT_OWNER")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_BENEFICIARY")));
    }

    private MockHttpServletRequest requestWithToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        return request;
    }

    private User user(String id, String role, String status) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setAccountStatus(status);
        return user;
    }
}
