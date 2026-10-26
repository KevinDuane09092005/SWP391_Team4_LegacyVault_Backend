package com.legacyvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@legacyvault.com}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] Mã xác thực OTP tài khoản");
            message.setText("Xin chào,\n\n"
                    + "Mã xác thực OTP của bạn cho hệ thống LegacyVault là: " + otpCode + "\n\n"
                    + "Mã này có hiệu lực trong vòng 5 phút.\n"
                    + "Vui lòng không chia sẻ mã này cho bất kỳ ai.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ bảo mật LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email chứa OTP thành công tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email OTP ({}); mã xác thực không được ghi log",
                    e.getClass().getSimpleName());
        }
    }

    @Override
    public void sendInvitationEmail(String toEmail, String contactName, String ownerName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] Thư mời tham gia xác lập di sản số");
            message.setText("Xin chào " + contactName + ",\n\n"
                    + (ownerName != null ? ownerName : "Một người thân") 
                    + " đã thêm bạn vào danh bạ tín nhiệm trên LegacyVault để chuẩn bị cho kế hoạch bàn giao di sản số.\n\n"
                    + "Vui lòng đăng ký tài khoản tại LegacyVault bằng email này để kích hoạt trạng thái xác thực và nhận các thông báo quan trọng.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email mời tham gia thành công tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi thư mời tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendDmsWarningEmail(String toEmail, String ownerName, int daysLeft) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] Cảnh báo: Sắp đến hạn điểm danh Dead Man's Switch");
            message.setText("Xin chào " + (ownerName != null ? ownerName : "Chủ tài sản") + ",\n\n"
                    + "Kỳ điểm danh an toàn Dead Man's Switch của bạn sắp kết thúc. "
                    + "Bạn còn khoảng " + daysLeft + " ngày để thực hiện điểm danh (Check-in).\n\n"
                    + "Vui lòng đăng nhập vào ứng dụng LegacyVault và nhấn nút 'Check-in Now' để xác nhận tình trạng an toàn và giữ tài sản được bảo vệ.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ bảo mật LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email cảnh báo sắp đến hạn DMS tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email cảnh báo DMS tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendDmsGracePeriodEmail(String toEmail, String recipientName, String ownerName, int graceDaysLeft, boolean isOwner) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            if (isOwner) {
                message.setSubject("[LegacyVault] KHẨN CẤP: Dead Man's Switch đã bước vào Thời gian Ân hạn (Grace Period)");
                message.setText("Kính gửi " + (recipientName != null ? recipientName : "Chủ tài sản") + ",\n\n"
                        + "Hệ thống ghi nhận bạn đã bỏ lỡ hạn chót điểm danh định kỳ. "
                        + "Thời gian ân hạn khẩn cấp (Grace Period) đã được kích hoạt và chỉ còn " + graceDaysLeft + " ngày.\n\n"
                        + "Nếu bạn an toàn, xin vui lòng ĐĂNG NHẬP NGAY và bấm 'Emergency Check-in' để hủy quy trình bàn giao di sản.\n"
                        + "Sau thời gian này, hồ sơ bàn giao sẽ tự động chuyển cho Người thi hành được chỉ định.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống bảo vệ LegacyVault");
            } else {
                message.setSubject("[LegacyVault] Thông báo: Chủ tài sản bước vào thời gian ân hạn Grace Period");
                message.setText("Kính gửi Người thi hành " + (recipientName != null ? recipientName : "") + ",\n\n"
                        + "Chủ tài sản " + (ownerName != null ? ownerName : "") + " chưa thực hiện điểm danh theo lịch trình và đã bước vào thời gian ân hạn (" + graceDaysLeft + " ngày còn lại).\n"
                        + "Hệ thống gửi thông báo này để bạn chuẩn bị và theo dõi tình hình.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống bảo vệ LegacyVault");
            }

            mailSender.send(message);
            logger.info("Đã gửi email Grace Period tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email Grace Period tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendHandoverTriggeredNotice(String toEmail, String executorName, String ownerName, String caseNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] KHẨN CẤP: Kích hoạt quy trình bàn giao di sản (Mã hồ sơ: " + caseNumber + ")");
            message.setText("Kính gửi Người thi hành " + (executorName != null ? executorName : "") + ",\n\n"
                    + "Thời gian ân hạn của chủ tài sản " + (ownerName != null ? ownerName : "") + " đã kết thúc mà không có tín hiệu phản hồi an toàn.\n\n"
                    + "Hệ thống LegacyVault đã tự động khởi tạo Hồ sơ Bàn giao Di sản Số với Mã hồ sơ: " + caseNumber + ".\n"
                    + "Vui lòng đăng nhập vào cổng Người thi hành để tiến hành tải lên tài liệu xác nhận pháp lý (Giấy chứng tử/xác nhận).\n\n"
                    + "Trân trọng,\n"
                    + "Hệ thống ủy quyền LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi thông báo kích hoạt bàn giao tới Executor: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi thông báo kích hoạt bàn giao tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendHandoverAccessCodeEmail(String toEmail, String beneficiaryName, String ownerName, String caseNumber, String accessCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault - Security Notification] Digital Estate Handover Authorized - " + caseNumber);
            message.setText("Dear " + (beneficiaryName != null ? beneficiaryName : "Beneficiary") + ",\n\n"
                    + "The legal verification process for the digital estate of " + (ownerName != null ? ownerName : "the Vault Owner")
                    + " (Case Ref: " + caseNumber + ") has been successfully verified and approved by the certified Legal Verifier.\n\n"
                    + "You are designated as an authorized recipient of digital inheritance assets.\n"
                    + "Your Secure Handover Access Code is:\n\n"
                    + "    " + accessCode + "\n\n"
                    + "Please proceed to the LegacyVault Beneficiary Access Portal to verify your identity and access your allocated assets.\n\n"
                    + "Sincerely,\n"
                    + "LegacyVault Security & Estate Administration");

            mailSender.send(message);
            logger.info("Đã gửi email Access Code tới Beneficiary: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email Access Code ({}); mã truy cập không được ghi log",
                    e.getClass().getSimpleName());
        }
    }
}

