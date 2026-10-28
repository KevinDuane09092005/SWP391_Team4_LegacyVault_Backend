package com.legacyvault.controller;

import com.legacyvault.dto.ContactResponse;
import com.legacyvault.dto.CreateContactRequest;
import com.legacyvault.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<?> createContact(
            Authentication authentication,
            @Valid @RequestBody CreateContactRequest request
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            ContactResponse contact = contactService.createContact(ownerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Contact saved and invitation email sent successfully",
                    "contact", contact
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/invitation/{contactId}/respond", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> respondToInvitation(
            @PathVariable("contactId") String contactId,
            @RequestParam(value = "decision", defaultValue = "accept") String decision
    ) {
        try {
            ContactResponse updated = contactService.respondToInvitation(contactId, decision);
            boolean accepted = "verified".equalsIgnoreCase(updated.getStatus());
            String title = accepted
                    ? "Đã xác nhận đồng ý liên hệ!"
                    : "Đã từ chối lời mời liên hệ";
            String badgeColor = accepted ? "#10b981" : "#ef4444";
            String description = accepted
                    ? "Cảm ơn <strong>" + updated.getFullName() + "</strong>! Bạn đã đồng ý trở thành Người liên hệ tin cậy trên hệ thống <strong>LegacyVault</strong>. Trạng thái của bạn trong danh bạ của chủ tài sản đã được chuyển sang <strong>Đã xác thực (Verified)</strong>."
                    : "Bạn đã từ chối lời mời trở thành Người liên hệ tin cậy trên hệ thống <strong>LegacyVault</strong>.";

            String html = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head>
                      <meta charset="UTF-8" />
                      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                      <title>LegacyVault - %s</title>
                    </head>
                    <body style="margin:0; padding:40px 16px; background:#090d16; color:#f8fafc; font-family:'Segoe UI',Arial,sans-serif; display:flex; justify-content:center; align-items:center; min-height:80vh;">
                      <div style="max-width:520px; width:100%%; background:#111827; border:1px solid #1f2937; border-radius:16px; padding:32px; text-align:center; box-shadow:0 20px 40px rgba(0,0,0,0.4);">
                        <div style="display:inline-block; background:%s; color:#052e16; font-weight:700; padding:6px 16px; border-radius:999px; font-size:13px; margin-bottom:16px;">
                          LegacyVault Contact Confirmation
                        </div>
                        <h1 style="margin:0 0 14px; font-size:24px;">%s</h1>
                        <p style="color:#cbd5e1; font-size:15px; line-height:1.6; margin-bottom:28px;">%s</p>
                        <a href="http://localhost:5173" style="display:inline-block; background:#10b981; color:#052e16; font-weight:700; text-decoration:none; padding:12px 24px; border-radius:8px; font-size:14px;">
                          Mở trang chủ LegacyVault
                        </a>
                      </div>
                    </body>
                    </html>
                    """.formatted(title, badgeColor, title, description);
            return ResponseEntity.ok(html);
        } catch (Exception e) {
            String errorHtml = """
                    <!DOCTYPE html>
                    <html lang="vi">
                    <head><meta charset="UTF-8" /><title>LegacyVault - Lỗi xác nhận</title></head>
                    <body style="margin:0; padding:40px 16px; background:#090d16; color:#f8fafc; font-family:'Segoe UI',Arial,sans-serif; text-align:center;">
                      <h2>Không thể xử lý yêu cầu</h2>
                      <p style="color:#94a3b8;">%s</p>
                    </body>
                    </html>
                    """.formatted(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorHtml);
        }
    }

    @GetMapping
    public ResponseEntity<?> getContacts(
            Authentication authentication,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            List<ContactResponse> contacts = contactService.getContacts(ownerId, keyword);
            return ResponseEntity.ok(contacts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{contactId}")
    public ResponseEntity<?> getContact(
            Authentication authentication,
            @PathVariable("contactId") String contactId
    ) {
        try {
            String ownerId = getAuthenticatedUserId(authentication);
            ContactResponse contact = contactService.getContactById(ownerId, contactId);
            return ResponseEntity.ok(contact);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private String getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Unauthorized: No user session found");
        }
        return authentication.getName();
    }
}
