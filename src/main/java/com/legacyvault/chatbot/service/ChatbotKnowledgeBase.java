package com.legacyvault.chatbot.service;

import com.legacyvault.chatbot.dto.ChatbotRole;
import com.legacyvault.chatbot.dto.ChatbotTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ChatbotKnowledgeBase {

    private static final Set<String> ALL_ROLES = Set.of("ALL");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    private final String productGuidance;
    private final List<Faq> faq = List.of(
            new Faq(ChatbotTopic.GENERAL, "What can the LegacyVault assistant help with?", "LegacyVault có thể hỗ trợ những gì?", ALL_ROLES, "help assistant chatbot legacyvault hỗ trợ trợ giúp"),
            new Faq(ChatbotTopic.ACCOUNT, "How do I manage my account details?", "Tôi quản lý thông tin tài khoản như thế nào?", ALL_ROLES, "account profile email password tài khoản hồ sơ email mật khẩu"),
            new Faq(ChatbotTopic.SECURITY, "What information should I avoid sharing in chat?", "Tôi không nên chia sẻ thông tin gì trong cuộc trò chuyện?", ALL_ROLES, "private privacy security password otp identity card sensitive bảo mật riêng tư mật khẩu mã otp căn cước"),
            new Faq(ChatbotTopic.VAULTS, "How do I create or organize a vault?", "Tôi tạo hoặc sắp xếp vault như thế nào?", Set.of("VAULT_OWNER", "ADMIN"), "vault create organize vaults tạo quản lý sắp xếp kho"),
            new Faq(ChatbotTopic.VAULTS, "Who can access a vault?", "Ai có thể truy cập vault?", Set.of("VAULT_OWNER", "ADMIN"), "vault access permission role vaults quyền truy cập ai"),
            new Faq(ChatbotTopic.ASSETS, "How do I add or organize a digital asset?", "Tôi thêm hoặc sắp xếp tài sản số như thế nào?", Set.of("VAULT_OWNER", "ADMIN"), "asset add upload organize digital tài sản thêm tải lên sắp xếp"),
            new Faq(ChatbotTopic.ASSETS, "How do I assign an asset to a beneficiary?", "Tôi chỉ định tài sản cho người thụ hưởng như thế nào?", Set.of("VAULT_OWNER", "ADMIN"), "asset assign beneficiary assignment tài sản chỉ định người thụ hưởng"),
            new Faq(ChatbotTopic.CONTACTS, "How do I add an executor or beneficiary?", "Tôi thêm người thực thi hoặc người thụ hưởng như thế nào?", Set.of("VAULT_OWNER", "EXECUTOR", "ADMIN"), "contact add executor beneficiary contact người liên hệ thêm người thực thi thụ hưởng"),
            new Faq(ChatbotTopic.CONTACTS, "What is the difference between an executor and a beneficiary?", "Người thực thi và người thụ hưởng khác nhau thế nào?", Set.of("VAULT_OWNER", "EXECUTOR", "BENEFICIARY", "ADMIN"), "executor beneficiary difference role người thực thi người thụ hưởng khác biệt vai trò"),
            new Faq(ChatbotTopic.DEAD_MANS_SWITCH, "What is the Dead Man's Switch?", "Dead Man's Switch là gì?", Set.of("VAULT_OWNER", "EXECUTOR", "ADMIN"), "dead mans switch check in checkin schedule switch điểm danh lịch"),
            new Faq(ChatbotTopic.DEAD_MANS_SWITCH, "What happens after a missed check-in?", "Điều gì xảy ra sau khi bỏ lỡ lần xác nhận?", Set.of("VAULT_OWNER", "EXECUTOR", "ADMIN"), "miss missed check in checkin grace timer switch bỏ lỡ xác nhận thời gian"),
            new Faq(ChatbotTopic.LEGAL_VERIFICATION, "What happens during legal verification?", "Quy trình xác minh pháp lý diễn ra như thế nào?", Set.of("EXECUTOR", "BENEFICIARY", "LEGAL_VERIFIER", "ADMIN"), "legal verification evidence review flow 5 pháp lý xác minh bằng chứng hồ sơ"),
            new Faq(ChatbotTopic.LEGAL_VERIFICATION, "What if more evidence is requested?", "Tôi cần làm gì nếu được yêu cầu bổ sung bằng chứng?", Set.of("EXECUTOR", "BENEFICIARY", "LEGAL_VERIFIER", "ADMIN"), "evidence more information resubmit request bổ sung bằng chứng thông tin gửi lại"),
            new Faq(ChatbotTopic.LEGAL_VERIFICATION, "Does legal approval release the assets?", "Được duyệt pháp lý có đồng nghĩa tài sản được bàn giao không?", Set.of("EXECUTOR", "BENEFICIARY", "LEGAL_VERIFIER", "ADMIN"), "legal approval release asset handover approve duyệt giải phóng tài sản bàn giao"),
            new Faq(ChatbotTopic.HANDOVER, "How does the beneficiary handover process work?", "Quy trình bàn giao cho người thụ hưởng hoạt động thế nào?", Set.of("EXECUTOR", "BENEFICIARY", "ADMIN"), "handover beneficiary flow 6 access process bàn giao người thụ hưởng truy cập quy trình"),
            new Faq(ChatbotTopic.HANDOVER, "Why is my handover request waiting for review?", "Vì sao yêu cầu bàn giao của tôi đang chờ xét duyệt?", Set.of("EXECUTOR", "BENEFICIARY", "ADMIN"), "handover request waiting pending review case status bàn giao chờ xét duyệt hồ sơ"),
            new Faq(ChatbotTopic.FACE_VERIFICATION, "Why does a beneficiary need face verification?", "Vì sao người thụ hưởng cần xác minh khuôn mặt?", Set.of("VAULT_OWNER", "BENEFICIARY", "ADMIN"), "face facial verification liveness beneficiary khuôn mặt xác minh người thụ hưởng sống"),
            new Faq(ChatbotTopic.FACE_VERIFICATION, "What if a beneficiary has not enrolled a face yet?", "Nếu người thụ hưởng chưa đăng ký khuôn mặt thì sao?", Set.of("VAULT_OWNER", "BENEFICIARY", "ADMIN"), "face enroll enrollment register stored template khuôn mặt đăng ký ghi danh mẫu"),
            new Faq(ChatbotTopic.FACE_VERIFICATION, "Does passing face verification guarantee access?", "Xác minh khuôn mặt thành công có đảm bảo được truy cập không?", Set.of("VAULT_OWNER", "BENEFICIARY", "ADMIN"), "face verification pass access guarantee khuôn mặt thành công truy cập đảm bảo"),
            new Faq(ChatbotTopic.BILLING, "How do I change my plan?", "Tôi đổi gói như thế nào?", Set.of("VAULT_OWNER", "ADMIN"), "billing plan subscription upgrade downgrade payment gói thanh toán đăng ký"),
            new Faq(ChatbotTopic.BILLING, "How do I manage my payment methods?", "Tôi quản lý phương thức thanh toán như thế nào?", Set.of("VAULT_OWNER", "ADMIN"), "billing payment method card transaction hóa đơn phương thức thẻ giao dịch"),
            new Faq(ChatbotTopic.SECURITY, "Can the chatbot see my vault or case information?", "Chatbot có xem được thông tin vault hoặc hồ sơ của tôi không?", ALL_ROLES, "chatbot vault case data privacy records access hồ sơ dữ liệu riêng tư truy cập"),
            new Faq(ChatbotTopic.SECURITY, "Can the chatbot give legal advice?", "Chatbot có thể đưa ra tư vấn pháp lý không?", ALL_ROLES, "legal advice inheritance law lawyer tư vấn pháp luật thừa kế luật sư")
    );

    public ChatbotKnowledgeBase(@Value("classpath:chatbot/product-guidance.md") Resource guidance) {
        try (var inputStream = guidance.getInputStream()) {
            this.productGuidance = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load chatbot product guidance.", exception);
        }
    }

    public String productGuidance() {
        return productGuidance;
    }

    public List<String> suggestions(String query, String language, ChatbotRole role, int limit) {
        List<Faq> relevant = faq.stream().filter(entry -> entry.visibleTo(role)).toList();
        if (query == null || query.isBlank()) {
            return relevant.stream().limit(limit).map(entry -> entry.question(language)).toList();
        }

        String normalizedQuery = normalize(query);
        List<ScoredFaq> matches = new ArrayList<>();
        for (int i = 0; i < relevant.size(); i++) {
            Faq entry = relevant.get(i);
            int score = entry.score(normalizedQuery);
            if (score > 0) {
                matches.add(new ScoredFaq(entry, score, i));
            }
        }
        return matches.stream()
                .sorted(Comparator.comparingInt(ScoredFaq::score).reversed().thenComparingInt(ScoredFaq::order))
                .limit(limit)
                .map(match -> match.faq().question(language))
                .toList();
    }

    public List<String> relatedQuestions(ChatbotTopic topic, String language, ChatbotRole role) {
        List<String> related = faq.stream()
                .filter(entry -> entry.topic() == topic && entry.visibleTo(role))
                .limit(3)
                .map(entry -> entry.question(language))
                .toList();
        return related.isEmpty() ? suggestions("", language, role, 3) : related;
    }

    private static String normalize(String value) {
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed)
                .replaceAll("")
                .toLowerCase(Locale.ROOT)
                .replace('đ', 'd')
                .trim();
    }

    private record ScoredFaq(Faq faq, int score, int order) {
    }

    private record Faq(ChatbotTopic topic, String english, String vietnamese, Set<String> roles, String keywords) {
        boolean visibleTo(ChatbotRole role) {
            return roles.contains("ALL") || roles.contains(role.name());
        }

        String question(String language) {
            return "vi".equals(language) ? vietnamese : english;
        }

        int score(String normalizedQuery) {
            int score = 0;
            String candidateText = normalize(english + " " + vietnamese + " " + keywords);
            if (candidateText.contains(normalizedQuery)) {
                score += 2;
            }
            for (String keyword : keywords.split("\\s+")) {
                String normalizedKeyword = normalize(keyword);
                if (normalizedKeyword.length() >= 2 && normalizedQuery.contains(normalizedKeyword)) {
                    score += normalizedKeyword.length() > 5 ? 3 : 1;
                }
            }
            return score;
        }
    }
}
