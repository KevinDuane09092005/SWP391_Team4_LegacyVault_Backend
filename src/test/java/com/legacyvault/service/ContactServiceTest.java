package com.legacyvault.service;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.entity.Contact;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ContactServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ContactService contactService;

    private CreateContactRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateContactRequest(
                "Tran Thi B",
                "Spouse",
                "tranthib@email.com",
                "Beneficiary"
        );
    }

    @Test
    void testCreateContact_UserExists_VerifiedStatus() {
        when(userRepository.existsByEmail("tranthib@email.com")).thenReturn(true);
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setContactId("contact-uuid-1");
            return c;
        });

        ContactResponse response = contactService.createContact("owner-123", request);

        assertNotNull(response);
        assertEquals("verified", response.getStatus());
        assertTrue(response.getIsBeneficiary());
        assertFalse(response.getIsExecutor());
        verify(emailService, never()).sendInvitationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testCreateContact_UserNotExists_PendingStatus_SendsEmail() {
        when(userRepository.existsByEmail("tranthib@email.com")).thenReturn(false);
        when(userRepository.findById("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setContactId("contact-uuid-1");
            return c;
        });

        ContactResponse response = contactService.createContact("owner-123", request);

        assertNotNull(response);
        assertEquals("pending", response.getStatus());
        verify(emailService, times(1)).sendInvitationEmail(eq("tranthib@email.com"), eq("Tran Thi B"), anyString());
    }

    @Test
    void testGetContacts_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com", 
                                      "0912345678", true, false, "verified");
        contact.setContactId("contact-1");
        when(contactRepository.findByOwnerId("owner-123")).thenReturn(List.of(contact));

        List<ContactResponse> list = contactService.getContacts("owner-123", null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Tran Thi B", list.get(0).getFullName());
    }
}
