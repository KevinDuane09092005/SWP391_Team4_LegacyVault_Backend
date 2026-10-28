package com.legacyvault.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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
            logger.warn("Không thể gửi email tới {}: {}. (Mã OTP phục vụ dev/test: {})",
                    toEmail, e.getMessage(), otpCode);
        }
    }

    @Override
    public void sendInvitationEmail(String toEmail, String contactName, String ownerName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] Lời mời tham gia bảo vệ di sản từ "
                    + (ownerName != null ? ownerName : "người thân"));
            message.setText("Xin chào " + (contactName != null ? contactName : "bạn") + ",\n\n"
                    + (ownerName != null ? ownerName : "Một người dùng")
                    + " đã thêm bạn vào danh bạ bảo vệ di sản số đáng tin cậy trên hệ thống LegacyVault.\n\n"
                    + "Vui lòng đăng ký tài khoản tại LegacyVault bằng email này để kích hoạt trạng thái xác thực và nhận các thông báo quan trọng.\n\n"
                    + "Trân trọng,\n"
                    + "Đội ngũ LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email mời tham gia thành công tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi thư mời tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
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

    @Async
    @Override
    public void sendDmsGracePeriodEmail(String toEmail, String recipientName, String ownerName, int graceDaysLeft,
            boolean isOwner) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            if (isOwner) {
                message.setSubject(
                        "[LegacyVault] KHẨN CẤP: Dead Man's Switch đã bước vào Thời gian Ân hạn (Grace Period)");
                message.setText("Kính gửi " + (recipientName != null ? recipientName : "Chủ tài sản") + ",\n\n"
                        + "Hệ thống ghi nhận bạn đã bỏ lỡ hạn chót điểm danh định kỳ. "
                        + "Thời gian Ân hạn khẩn cấp (Grace Period) đã được kích hoạt và chỉ còn " + graceDaysLeft
                        + " ngày.\n\n"
                        + "Nếu bạn an toàn, xin vui lòng ĐĂNG NHẬP NGAY và bấm 'Emergency Check-in' để hủy quy trình bàn giao di sản.\n"
                        + "Sau thời gian này, hồ sơ bàn giao sẽ tự động chuyển cho Người thi hành được chỉ định.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống bảo vệ LegacyVault");
            } else {
                message.setSubject("[LegacyVault] Thông báo: Chủ tài sản bước vào thời gian Ân hạn Grace Period");
                message.setText("Kính gửi Người thi hành " + (recipientName != null ? recipientName : "") + ",\n\n"
                        + "Chủ tài sản " + (ownerName != null ? ownerName : "")
                        + " chưa thực hiện điểm danh theo lịch trình và đã bước vào thời gian Ân hạn (" + graceDaysLeft
                        + " ngày còn lại).\n"
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

    @Async
    @Override
    public void sendHandoverTriggeredNotice(String toEmail, String executorName, String ownerName, String caseNumber) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(
                    "[LegacyVault] KHẨN CẤP: Kích hoạt quy trình bàn giao di sản (Mã hồ sơ: " + caseNumber + ")");
            message.setText("Kính gửi Người thi hành " + (executorName != null ? executorName : "") + ",\n\n"
                    + "Thời gian Ân hạn của chủ tài sản " + (ownerName != null ? ownerName : "")
                    + " đã kết thúc mà không có tín hiệu phản hồi an toàn.\n\n"
                    + "Hệ thống LegacyVault đã tự động khởi tạo Hồ sơ Bàn giao Di sản Số với Mã hồ sơ: " + caseNumber
                    + ".\n"
                    + "Vui lòng đăng nhập vào cổng Người thi hành để tiến hành tải lên tài liệu xác nhận pháp lý (Giấy chứng tử/xác nhận).\n\n"
                    + "Trân trọng,\n"
                    + "Hệ thống Ủy quyền LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi thông báo kích hoạt bàn giao tới Executor: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi thông báo kích hoạt bàn giao tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendHandoverAccessCodeEmail(String toEmail, String beneficiaryName, String ownerName, String caseNumber,
            String accessCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("[LegacyVault] Thông báo bàn giao di sản số - Mã hồ sơ: " + caseNumber);
            message.setText("Kính gửi " + (beneficiaryName != null ? beneficiaryName : "Người thụ hưởng") + ",\n\n"
                    + "Hồ sơ bàn giao di sản số của " + (ownerName != null ? ownerName : "Chủ tài sản")
                    + " (Mã hồ sơ: " + caseNumber
                    + ") đã được phê duyệt pháp lý và Người thi hành (Executor) đã chính thức kích hoạt bàn giao.\n\n"
                    + "Bạn được chỉ định là người thụ hưởng hợp pháp của các tài sản số này.\n\n"
                    + "THÔNG TIN TIẾP NHẬN DI SẢN:\n"
                    + "  - Mã hồ sơ: " + caseNumber + "\n"
                    + "  - Mã truy cập bảo mật (Access Code): " + accessCode + "\n\n"
                    + "Vui lòng truy cập Cổng Người thụ hưởng (Beneficiary Portal) của LegacyVault để nhập mã truy cập, hoàn tất xác thực danh tính và mở khóa tài sản được ủy thác.\n\n"
                    + "Trân trọng,\n"
                    + "Hệ thống Quản trị & Bàn giao Di sản Số LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email Access Code tới Beneficiary: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email Access Code tới {}: {}. (Mã Access Code phục vụ dev/test: {})",
                    toEmail, e.getMessage(), accessCode);
        }
    }

    @Override
    public void sendVerificationApprovedToExecutor(String executorEmail, String executorName,
            String caseNumber, String beneficiaryEmail,
            String beneficiaryName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(executorEmail);
            message.setSubject("[LegacyVault] Hồ sơ bàn giao đã được phê duyệt pháp lý - Mã hồ sơ: " + caseNumber);
            message.setText(
                    "Kính gửi Người thi hành " + (executorName != null ? executorName : "") + ",\n\n"
                            + "Hồ sơ bàn giao di sản số với Mã hồ sơ: " + caseNumber
                            + " đã được Bộ phận Xác minh Pháp lý phê duyệt thành công.\n\n"
                            + "Thông tin người thụ hưởng:\n"
                            + "  - Email: " + (beneficiaryEmail != null ? beneficiaryEmail : "Chưa cập nhật") + "\n"
                            + (beneficiaryName != null ? "  - Họ tên: " + beneficiaryName + "\n" : "")
                            + "\nVui lòng đăng nhập vào cổng Người thi hành để tiến hành các bước bàn giao tài sản.\n\n"
                            + "Lưu ý: Email này chỉ được gửi đến Người thi hành. "
                            + "Người thụ hưởng sẽ nhận thông báo riêng ở bước tiếp theo.\n\n"
                            + "Trân trọng,\n"
                            + "Hệ thống Ủy quyền LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email thông báo phê duyệt hồ sơ {} tới Executor: {}", caseNumber, executorEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email phê duyệt hồ sơ {} tới Executor {}: {}", caseNumber, executorEmail,
                    e.getMessage());
        }
    }

    @Override
    public void sendVerificationRejectedToExecutor(String executorEmail, String executorName,
            String caseNumber, String reason,
            String reasonCode) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(executorEmail);
            message.setSubject(
                    "[LegacyVault] Thông báo: Hồ sơ bàn giao bị từ chối thẩm định pháp lý - Mã hồ sơ: " + caseNumber);
            message.setText(
                    "Kính gửi Người thi hành " + (executorName != null ? executorName : "") + ",\n\n"
                            + "Hồ sơ bàn giao di sản số với Mã hồ sơ: " + caseNumber
                            + " đã bị Thẩm định viên pháp lý TỪ CHỐI sau khi xem xét tài liệu.\n\n"
                            + "CHI TIẾT TỪ CHỐI:\n"
                            + "  - Mã hồ sơ: " + caseNumber + "\n"
                            + "  - Mã lý do: " + (reasonCode != null ? reasonCode : "INVALID_DOCUMENT") + "\n"
                            + "  - Lý do cụ thể: "
                            + (reason != null ? reason
                                    : "Tài liệu chứng tử không hợp lệ hoặc không đủ điều kiện pháp lý.")
                            + "\n\n"
                            + "HƯỚNG DẪN XỬ LÝ:\n"
                            + "Vui lòng kiểm tra lại tài liệu pháp lý và tải lên lại tài liệu hợp lệ qua Cổng Người thi hành (Executor Console) để Thẩm định viên xem xét lại.\n\n"
                            + "Trân trọng,\n"
                            + "Hệ thống Quản trị & Bàn giao Di sản Số LegacyVault");

            mailSender.send(message);
            logger.info("Đã gửi email thông báo từ chối hồ sơ {} tới Executor: {}", caseNumber, executorEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email từ chối hồ sơ {} tới Executor {}: {}", caseNumber, executorEmail,
                    e.getMessage());
        }
    }
}