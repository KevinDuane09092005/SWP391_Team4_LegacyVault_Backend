package com.legacyvault.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode);
    void sendInvitationEmail(String toEmail, String contactName, String ownerName);
    void sendDmsWarningEmail(String toEmail, String ownerName, int daysLeft);
    void sendDmsGracePeriodEmail(String toEmail, String recipientName, String ownerName, int graceDaysLeft, boolean isOwner);
    void sendHandoverTriggeredNotice(String toEmail, String executorName, String ownerName, String caseNumber);
    void sendHandoverAccessCodeEmail(String toEmail, String beneficiaryName, String ownerName, String caseNumber, String accessCode);

    /**
     * Gửi thông báo đến Executor sau khi hồ sơ được phê duyệt pháp lý.
     * Nội dung bao gồm: mã hồ sơ bàn giao và email của người hưởng thụ.
     * Người hưởng thụ KHÔNG nhận mail này.
     */
    void sendVerificationApprovedToExecutor(String executorEmail, String executorName,
                                            String caseNumber, String beneficiaryEmail, String beneficiaryName);
}
