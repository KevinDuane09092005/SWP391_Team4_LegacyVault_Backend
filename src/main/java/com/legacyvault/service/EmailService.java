package com.legacyvault.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
    void sendInvitationEmail(String toEmail, String contactName, String ownerName);
    void sendDmsWarningEmail(String toEmail, String ownerName, int daysLeft);
    void sendDmsGracePeriodEmail(String toEmail, String recipientName, String ownerName, int graceDaysLeft, boolean isOwner);
    void sendHandoverTriggeredNotice(String toEmail, String executorName, String ownerName, String caseNumber);
}
