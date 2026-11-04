package com.legacyvault.service;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.entity.Contact;
import com.legacyvault.entity.DeadMansSwitch;
import com.legacyvault.entity.User;
import com.legacyvault.repository.AssetAssignmentRepository;
import com.legacyvault.repository.ContactRepository;
import com.legacyvault.repository.DeadMansSwitchRepository;
import com.legacyvault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Mock
    private AssetAssignmentRepository assetAssignmentRepository;

    @Mock
    private DeadMansSwitchRepository dmsRepository;

    private ContactService contactService;

    private CreateContactRequest request;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(
                contactRepository,
                userRepository,
                emailService,
                assetAssignmentRepository,
                dmsRepository
        );
        request = new CreateContactRequest(
                "Tran Thi B",
                "Spouse",
                "tranthib@email.com",
                "Beneficiary"
        );
    }

    @Test
    void testCreateContact_StartsAsPendingAndSendsInvitationEmail() {
        when(contactRepository.existsByOwnerIdAndEmailIgnoreCase("owner-123", "tranthib@email.com")).thenReturn(false);
        when(userRepository.findById("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> {
            Contact c = invocation.getArgument(0);
            c.setContactId("contact-uuid-1");
            return c;
        });

        ContactResponse response = contactService.createContact("owner-123", request);

        assertNotNull(response);
        assertEquals("pending", response.getStatus());
        assertTrue(response.getIsBeneficiary());
        assertFalse(response.getIsExecutor());
        verify(emailService, times(1)).sendContactInvitationEmail(
                eq("tranthib@email.com"),
                eq("Tran Thi B"),
                anyString(),
                eq("Spouse"),
                eq("Beneficiary"),
                eq("contact-uuid-1")
        );
    }

    @Test
    void testCreateContact_SelfInvitation_ThrowsException() {
        User owner = new User();
        owner.setFullName("Owner Name");
        owner.setEmail("tranthib@email.com");
        when(userRepository.findById("owner-123")).thenReturn(Optional.of(owner));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                contactService.createContact("owner-123", request)
        );

        assertTrue(ex.getMessage().contains("không thể tự thêm email của chính mình"));
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testCreateContact_DuplicateEmail_ThrowsException() {
        when(userRepository.findById("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.existsByOwnerIdAndEmailIgnoreCase("owner-123", "tranthib@email.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                contactService.createContact("owner-123", request)
        );

        assertTrue(ex.getMessage().contains("không được trùng email"));
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testRespondToInvitation_AcceptSetsVerified() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "pending");
        contact.setContactId("contact-uuid-1");
        when(contactRepository.findById("contact-uuid-1")).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.respondToInvitation("contact-uuid-1", "accept");

        assertEquals("verified", response.getStatus());
    }

    @Test
    void testRespondToInvitation_RejectSetsRejected() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "pending");
        contact.setContactId("contact-uuid-1");
        when(contactRepository.findById("contact-uuid-1")).thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.respondToInvitation("contact-uuid-1", "reject");

        assertEquals("rejected", response.getStatus());
    }

    @Test
    void testRespondToInvitation_ThrowsException_WhenRevoked() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "revoked");
        contact.setContactId("contact-uuid-1");
        when(contactRepository.findById("contact-uuid-1")).thenReturn(Optional.of(contact));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                contactService.respondToInvitation("contact-uuid-1", "accept")
        );

        assertTrue(ex.getMessage().contains("đã bị chủ tài sản thu hồi"));
        verify(contactRepository, never()).save(any());
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

    @Test
    void testRevokeContactInvitation_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "pending");
        contact.setContactId("contact-uuid-1");

        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.revokeContactInvitation("owner-123", "contact-uuid-1");

        assertNotNull(response);
        assertEquals("revoked", response.getStatus());
        verify(contactRepository, times(1)).save(contact);
    }

    @Test
    void testRevokeContactInvitation_ThrowsException_WhenNotPending() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "verified");
        contact.setContactId("contact-uuid-1");

        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                contactService.revokeContactInvitation("owner-123", "contact-uuid-1")
        );

        assertTrue(ex.getMessage().contains("Chỉ có thể thu hồi lời mời liên hệ đang ở trạng thái chờ"));
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testRevokeContactAccess_VerifiedContact_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Lawyer", "tranthib@email.com",
                null, false, true, "verified");
        contact.setContactId("contact-uuid-1");

        when(dmsRepository.findByOwnerId("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.revokeContactAccess("owner-123", "contact-uuid-1");

        assertNotNull(response);
        assertEquals("revoked", response.getStatus());
        verify(contactRepository, times(1)).save(contact);
    }

    @Test
    void testRevokeContactAccess_WhenDmsTriggered_ThrowsException() {
        DeadMansSwitch dms = new DeadMansSwitch();
        dms.setOwnerId("owner-123");
        dms.setStatus("triggered");

        when(dmsRepository.findByOwnerId("owner-123")).thenReturn(Optional.of(dms));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                contactService.revokeContactAccess("owner-123", "contact-uuid-1")
        );

        assertTrue(ex.getMessage().contains("Dead Man's Switch đã được kích hoạt"));
        verify(contactRepository, never()).save(any());
    }

    @Test
    void testResendContactInvitation_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "revoked");
        contact.setContactId("contact-uuid-1");

        when(dmsRepository.findByOwnerId("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(userRepository.findById("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ContactResponse response = contactService.resendContactInvitation("owner-123", "contact-uuid-1");

        assertEquals("pending", response.getStatus());
        verify(emailService, times(1)).sendContactInvitationEmail(
                eq("tranthib@email.com"),
                eq("Tran Thi B"),
                anyString(),
                eq("Spouse"),
                eq("Beneficiary"),
                eq("contact-uuid-1")
        );
    }

    @Test
    void testUpdateContact_UpdatesRoleAndInfo() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                "0901112222", true, false, "verified");
        contact.setContactId("contact-uuid-1");

        when(dmsRepository.findByOwnerId("owner-123")).thenReturn(Optional.empty());
        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(contactRepository.save(any(Contact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateContactRequest updateReq = new CreateContactRequest(
                "Tran Thi B Updated",
                "Wife & Executor",
                "tranthib@email.com",
                "Both"
        );
        updateReq.setPhoneNumber("0988889999");

        ContactResponse response = contactService.updateContact("owner-123", "contact-uuid-1", updateReq);

        assertEquals("Tran Thi B Updated", response.getFullName());
        assertTrue(response.getIsBeneficiary());
        assertTrue(response.getIsExecutor());
    }

    @Test
    void testDeleteContact_AssignedAsExecutor_ThrowsException() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Lawyer", "tranthib@email.com",
                null, false, true, "verified");
        contact.setContactId("contact-uuid-1");

        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(assetAssignmentRepository.existsByExecutorContactId("contact-uuid-1")).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                contactService.deleteContact("owner-123", "contact-uuid-1")
        );

        assertTrue(ex.getMessage().contains("đang được chỉ định làm Người thực thi"));
        verify(contactRepository, never()).delete(any());
    }

    @Test
    void testDeleteContact_Success() {
        Contact contact = new Contact("owner-123", "Tran Thi B", "Spouse", "tranthib@email.com",
                null, true, false, "verified");
        contact.setContactId("contact-uuid-1");

        when(contactRepository.findByContactIdAndOwnerId("contact-uuid-1", "owner-123"))
                .thenReturn(Optional.of(contact));
        when(assetAssignmentRepository.existsByExecutorContactId("contact-uuid-1")).thenReturn(false);

        assertDoesNotThrow(() -> contactService.deleteContact("owner-123", "contact-uuid-1"));

        verify(contactRepository, times(1)).delete(contact);
    }
}
