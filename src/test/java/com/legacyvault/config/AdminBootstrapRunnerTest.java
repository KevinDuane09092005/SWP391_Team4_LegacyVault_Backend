package com.legacyvault.config;

import com.legacyvault.entity.User;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminBootstrapRunnerTest {

    @Test
    void disabledBootstrapDoesNotCreateAnAccount() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);

        new AdminBootstrapRunner(repository, encoder, false, "", "", "").run(null);

        verifyNoInteractions(repository, encoder);
    }

    @Test
    void enabledBootstrapCreatesVerifiedAdminWithHashedPassword() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.existsByEmail("admin@example.com")).thenReturn(false);
        when(encoder.encode("a-long-demo-password")) .thenReturn("bcrypt-hash");
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new AdminBootstrapRunner(repository, encoder, true, " Admin@Example.com ", "Demo Admin",
                "a-long-demo-password").run(null);

        verify(repository).save(argThat(user ->
                "admin@example.com".equals(user.getEmail())
                        && "ADMIN".equals(user.getRole())
                        && Boolean.TRUE.equals(user.getIsVerified())
                        && Boolean.TRUE.equals(user.getIsActive())
                        && "bcrypt-hash".equals(user.getPasswordHash())));
        verify(encoder).encode("a-long-demo-password");
    }

    @Test
    void enabledBootstrapRequiresStrongPassword() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        AdminBootstrapRunner runner = new AdminBootstrapRunner(repository, encoder, true,
                "admin@example.com", "Demo Admin", "short");

        assertThrows(IllegalStateException.class, () -> runner.run(null));
        verifyNoInteractions(repository, encoder);
    }

    @Test
    void bootstrapNeverElevatesAnExistingAccount() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.existsByEmail("admin@example.com")).thenReturn(true);

        new AdminBootstrapRunner(repository, encoder, true, "admin@example.com", "Demo Admin",
                "a-long-demo-password").run(null);

        verify(repository, never()).save(any());
        verifyNoInteractions(encoder);
    }
}
