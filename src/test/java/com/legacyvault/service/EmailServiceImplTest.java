package com.legacyvault.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
    }

    @Test
    void testSendOtpEmail_Success() {
        emailService.sendOtpEmail("user@example.com", "654321");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("user@example.com", Objects.requireNonNull(sentMessage.getTo())[0]);
        assertTrue(Objects.requireNonNull(sentMessage.getSubject()).contains("OTP"));
        assertTrue(Objects.requireNonNull(sentMessage.getText()).contains("654321"));
    }

    @Test
    void testSendOtpEmail_FailureGracefullyHandled() {
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception
        assertDoesNotThrow(() -> emailService.sendOtpEmail("user@example.com", "654321"));
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendDmsWarningEmail_TestingMode_ContainsDmsLinkAndNoRemainingTime() {
        emailService.sendDmsWarningEmail("owner@example.com", "Thanh Pham", 0);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        String text = Objects.requireNonNull(msg.getText());
        assertTrue(text.contains("http://localhost:5173/dashboard/owner/dms"));
        assertFalse(text.contains("còn khoảng"));
    }

    @Test
    void testSendDmsGracePeriodEmail_TestingMode_ContainsDmsLinkAndNoRemainingTime() {
        emailService.sendDmsGracePeriodEmail("owner@example.com", "Thanh Pham", "Thanh Pham", 0, true);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage msg = captor.getValue();
        String text = Objects.requireNonNull(msg.getText());
        assertTrue(text.contains("http://localhost:5173/dashboard/owner/dms"));
        assertFalse(text.contains("còn khoảng"));
    }
}
