package com.legacyvault.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@legacyvault.com}")
    private String fromEmail;

    @Value("${app.backend-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendBaseUrl;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private String getFrontendDmsUrl() {
        String base = (frontendBaseUrl != null && !frontendBaseUrl.isBlank()) ? frontendBaseUrl.trim() : "http://localhost:5173";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/dashboard/owner/dms";
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

    @Async
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
    public void sendContactInvitationEmail(String toEmail, String contactName, String ownerName,
                                           String relationship, String expectedRole, String contactId) {
        String safeContactName = contactName != null ? contactName : "bạn";
        String safeOwnerName = ownerName != null ? ownerName : "Chủ tài sản";
        String safeRelationship = relationship != null ? relationship : "Người thân";
        String safeRole = expectedRole != null ? expectedRole : "Beneficiary";
        String baseUrl = (backendBaseUrl != null && !backendBaseUrl.isBlank()) ? backendBaseUrl : "http://localhost:8080";
        String acceptUrl = baseUrl + "/api/v1/contacts/invitation/" + contactId + "/respond?decision=accept";
        String rejectUrl = baseUrl + "/api/v1/contacts/invitation/" + contactId + "/respond?decision=reject";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                sendInvitationEmail(toEmail, contactName, ownerName);
                return;
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail != null ? fromEmail : "noreply@legacyvault.com");
            helper.setTo(toEmail);
            helper.setSubject("[LegacyVault] Lời mời trở thành Người liên hệ tin cậy từ " + safeOwnerName);

            String html = """
                    <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #0f172a; color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #1e293b;">
                      <div style="background: #10b981; padding: 20px 28px; color: #052e16;">
                        <h2 style="margin: 0; font-size: 20px;">LegacyVault — Xác nhận Người liên hệ tin cậy</h2>
                      </div>
                      <div style="padding: 28px;">
                        <p style="font-size: 16px; line-height: 1.6; margin-top: 0;">Xin chào <strong>%s</strong>,</p>
                        <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                          <strong>%s</strong> vừa thêm bạn vào danh bạ Người liên hệ tin cậy (Trusted Contact) trên hệ thống bảo vệ di sản số <strong>LegacyVault</strong>.
                        </p>
                        <div style="background: #1e293b; border-radius: 8px; padding: 16px; margin: 20px 0; font-size: 14px; line-height: 1.8;">
                          <div>• <strong>Người mời:</strong> %s</div>
                          <div>• <strong>Mối quan hệ:</strong> %s</div>
                          <div>• <strong>Vai trò chỉ định:</strong> %s</div>
                        </div>
                        <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                          Vui lòng chọn một trong hai nút dưới đây để xác nhận bạn có đồng ý tham gia vào danh bạ liên hệ của <strong>%s</strong> hay không:
                        </p>
                        <div style="margin: 28px 0; text-align: center;">
                          <a href="%s" style="display: inline-block; background: #10b981; color: #052e16; font-weight: 700; text-decoration: none; padding: 12px 24px; border-radius: 8px; margin-right: 12px; font-size: 15px;">
                            ✓ Đồng ý thêm vào liên hệ
                          </a>
                          <a href="%s" style="display: inline-block; background: #ef4444; color: #ffffff; font-weight: 600; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-size: 15px;">
                            ✕ Từ chối
                          </a>
                        </div>
                        <p style="font-size: 12px; color: #94a3b8; margin-top: 24px; border-top: 1px solid #1e293b; padding-top: 16px;">
                          Nếu nút bấm không hoạt động, bạn có thể mở trực tiếp đường dẫn sau để đồng ý:<br/>
                          <a href="%s" style="color: #38bdf8;">%s</a>
                        </p>
                      </div>
                    </div>
                    """.formatted(
                    safeContactName,
                    safeOwnerName,
                    safeOwnerName,
                    safeRelationship,
                    safeRole,
                    safeOwnerName,
                    acceptUrl,
                    rejectUrl,
                    acceptUrl,
                    acceptUrl
            );

            helper.setText(html, true);
            mailSender.send(mimeMessage);
            logger.info("Đã gửi email xác nhận liên hệ (HTML) thành công tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email xác nhận liên hệ tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendDmsWarningEmail(String toEmail, String ownerName, int daysLeft) {
        String safeOwnerName = ownerName != null ? ownerName : "Chủ tài khoản";
        String dmsUrl = getFrontendDmsUrl();
        String subject = "[LegacyVault] Cảnh báo: Sắp hết thời gian Check-in Dead Man's Switch";
        String timeNoticePlain = daysLeft > 0
                ? "Chu kỳ điểm danh an toàn Dead Man's Switch của bạn sắp kết thúc (còn khoảng " + daysLeft + " ngày).\n\n"
                : "Thời gian điểm danh định kỳ (Check-in Interval) trên công tắc an toàn Dead Man's Switch của bạn sắp kết thúc.\n\n";
        String plainText = "Xin chào " + safeOwnerName + ",\n\n"
                + timeNoticePlain
                + "Vui lòng truy cập trang Dead Man's Switch theo đường dẫn dưới đây và nhấn nút Check-in để xác nhận bạn vẫn an toàn:\n"
                + dmsUrl + "\n\n"
                + "Trân trọng,\n"
                + "Đội ngũ bảo mật LegacyVault";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject(subject);
                message.setText(plainText);
                mailSender.send(message);
                logger.info("Đã gửi email cảnh báo sắp hết hạn Check-in Interval tới: {}", toEmail);
                return;
            }

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail != null ? fromEmail : "noreply@legacyvault.com");
            helper.setTo(toEmail);
            helper.setSubject(subject);

            String timeNoticeHtml = daysLeft > 0
                    ? "Thời gian điểm danh định kỳ (<strong>Check-in Interval</strong>) trên công tắc an toàn <strong>Dead Man's Switch</strong> của bạn sắp kết thúc (còn khoảng <strong>" + daysLeft + " ngày</strong>)."
                    : "Thời gian điểm danh định kỳ (<strong>Check-in Interval</strong>) trên công tắc an toàn <strong>Dead Man's Switch</strong> của bạn sắp kết thúc.";

            String html = """
                    <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #0f172a; color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #1e293b;">
                      <div style="background: #f59e0b; padding: 20px 28px; color: #0f172a;">
                        <h2 style="margin: 0; font-size: 20px;">LegacyVault — Nhắc nhở Check-in Dead Man's Switch</h2>
                      </div>
                      <div style="padding: 28px;">
                        <p style="font-size: 16px; line-height: 1.6; margin-top: 0;">Xin chào <strong>%s</strong>,</p>
                        <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                          %s
                        </p>
                        <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                          Vui lòng nhấn vào nút bên dưới để chuyển đến trang <strong>Dead Man's Switch</strong> và thực hiện <strong>Check-in</strong> xác nhận bạn vẫn an toàn:
                        </p>
                        <div style="margin: 28px 0; text-align: center;">
                          <a href="%s" style="display: inline-block; background: #f59e0b; color: #0f172a; font-weight: 700; text-decoration: none; padding: 13px 26px; border-radius: 8px; font-size: 15px;">
                            Vào trang Dead Man's Switch để Check-in
                          </a>
                        </div>
                        <p style="font-size: 12px; color: #94a3b8; margin-top: 24px; border-top: 1px solid #1e293b; padding-top: 16px;">
                          Hoặc truy cập trực tiếp đường dẫn sau:<br/>
                          <a href="%s" style="color: #38bdf8;">%s</a>
                        </p>
                      </div>
                    </div>
                    """.formatted(safeOwnerName, timeNoticeHtml, dmsUrl, dmsUrl, dmsUrl);

            helper.setText(html, true);
            mailSender.send(mimeMessage);
            logger.info("Đã gửi email cảnh báo sắp hết hạn Check-in Interval (HTML) tới: {}", toEmail);
        } catch (Exception e) {
            logger.warn("Không thể gửi email cảnh báo DMS tới {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    @Override
    public void sendDmsGracePeriodEmail(String toEmail, String recipientName, String ownerName, int graceDaysLeft,
            boolean isOwner) {
        String safeRecipient = recipientName != null ? recipientName : (isOwner ? "Chủ tài khoản" : "Người thi hành");
        String safeOwner = ownerName != null ? ownerName : "Chủ tài khoản";
        String dmsUrl = getFrontendDmsUrl();

        try {
            if (isOwner) {
                String subject = "[LegacyVault] KHẨN CẤP: Sắp hết thời gian Ân hạn (Grace Period) Dead Man's Switch";
                String timeNoticePlain = graceDaysLeft > 0
                        ? "Thời gian Ân hạn khẩn cấp (Grace Period) trên công tắc an toàn Dead Man's Switch của bạn sắp kết thúc (còn khoảng " + graceDaysLeft + " ngày).\n\n"
                        : "Thời gian Ân hạn khẩn cấp (Grace Period) trên công tắc an toàn Dead Man's Switch của bạn sắp kết thúc.\n\n";
                String plainText = "Kính gửi " + safeRecipient + ",\n\n"
                        + timeNoticePlain
                        + "Nếu bạn vẫn an toàn, vui lòng truy cập ngay trang Dead Man's Switch theo đường dẫn dưới đây và nhấn nút Check-in để hủy kích hoạt quy trình bàn giao di sản:\n"
                        + dmsUrl + "\n\n"
                        + "Sau khi hết thời gian ân hạn, hồ sơ bàn giao sẽ tự động được kích hoạt.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống bảo vệ LegacyVault";

                MimeMessage mimeMessage = mailSender.createMimeMessage();
                if (mimeMessage == null) {
                    SimpleMailMessage message = new SimpleMailMessage();
                    message.setFrom(fromEmail);
                    message.setTo(toEmail);
                    message.setSubject(subject);
                    message.setText(plainText);
                    mailSender.send(message);
                    logger.info("Đã gửi email cảnh báo sắp hết hạn Grace Period tới chủ tài khoản: {}", toEmail);
                    return;
                }

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setFrom(fromEmail != null ? fromEmail : "noreply@legacyvault.com");
                helper.setTo(toEmail);
                helper.setSubject(subject);

                String timeNoticeHtml = graceDaysLeft > 0
                        ? "Thời gian Ân hạn khẩn cấp (<strong>Grace Period</strong>) trên công tắc an toàn <strong>Dead Man's Switch</strong> của bạn sắp kết thúc (còn khoảng <strong>" + graceDaysLeft + " ngày</strong>)."
                        : "Thời gian Ân hạn khẩn cấp (<strong>Grace Period</strong>) trên công tắc an toàn <strong>Dead Man's Switch</strong> của bạn sắp kết thúc.";

                String html = """
                        <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #0f172a; color: #f8fafc; border-radius: 12px; overflow: hidden; border: 1px solid #1e293b;">
                          <div style="background: #ef4444; padding: 20px 28px; color: #ffffff;">
                            <h2 style="margin: 0; font-size: 20px;">LegacyVault — Cảnh báo Khẩn cấp (Grace Period)</h2>
                          </div>
                          <div style="padding: 28px;">
                            <p style="font-size: 16px; line-height: 1.6; margin-top: 0;">Kính gửi <strong>%s</strong>,</p>
                            <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                              %s
                            </p>
                            <p style="font-size: 15px; line-height: 1.6; color: #cbd5e1;">
                              Nếu bạn vẫn an toàn, vui lòng nhấn ngay vào nút bên dưới để chuyển đến trang <strong>Dead Man's Switch</strong> và thực hiện <strong>Check-in</strong> nhằm hủy quy trình bàn giao di sản:
                            </p>
                            <div style="margin: 28px 0; text-align: center;">
                              <a href="%s" style="display: inline-block; background: #ef4444; color: #ffffff; font-weight: 700; text-decoration: none; padding: 13px 26px; border-radius: 8px; font-size: 15px;">
                                Vào trang Dead Man's Switch để Check-in ngay
                              </a>
                            </div>
                            <p style="font-size: 12px; color: #94a3b8; margin-top: 24px; border-top: 1px solid #1e293b; padding-top: 16px;">
                              Hoặc truy cập trực tiếp đường dẫn sau:<br/>
                              <a href="%s" style="color: #38bdf8;">%s</a>
                            </p>
                          </div>
                        </div>
                        """.formatted(safeRecipient, timeNoticeHtml, dmsUrl, dmsUrl, dmsUrl);

                helper.setText(html, true);
                mailSender.send(mimeMessage);
                logger.info("Đã gửi email cảnh báo sắp hết hạn Grace Period (HTML) tới chủ tài khoản: {}", toEmail);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(toEmail);
                message.setSubject("[LegacyVault] Thông báo: Chủ tài sản bước vào thời gian Ân hạn Grace Period");
                String executorTimeText = graceDaysLeft > 0
                        ? " và đang trong thời gian Ân hạn (" + graceDaysLeft + " ngày còn lại).\n"
                        : " và thời gian Ân hạn (Grace Period) sắp kết thúc.\n";
                message.setText("Kính gửi Người thi hành " + safeRecipient + ",\n\n"
                        + "Chủ tài sản " + safeOwner
                        + " chưa thực hiện điểm danh theo lịch trình" + executorTimeText
                        + "Hệ thống gửi thông báo này để bạn chuẩn bị và theo dõi tình hình.\n\n"
                        + "Trân trọng,\n"
                        + "Hệ thống bảo vệ LegacyVault");
                mailSender.send(message);
                logger.info("Đã gửi email Grace Period tới Executor: {}", toEmail);
            }
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
