package com.legacyvault.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.legacyvault.chatbot.config.ChatbotProperties;
import com.legacyvault.chatbot.dto.ChatbotAiReply;
import com.legacyvault.chatbot.dto.ChatbotHistoryTurn;
import com.legacyvault.chatbot.dto.ChatbotReplyStatus;
import com.legacyvault.chatbot.dto.ChatbotTopic;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class OpenAiResponsesClient implements ChatbotAiProvider {

    private static final String RESPONSE_SCHEMA_NAME = "legacyvault_chatbot_reply";
    private final RestClient restClient;
    private final ChatbotProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiResponsesClient(RestClient chatbotRestClient,
                                 ChatbotProperties properties,
                                 ObjectMapper objectMapper) {
        this.restClient = chatbotRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatbotAiReply answer(String instructions, String message, List<ChatbotHistoryTurn> history) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw ChatbotException.notConfigured();
        }

        ObjectNode request = createRequest(instructions, message, history);
        JsonNode response;
        try {
            response = restClient.post()
                    .uri(endpointUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw ChatbotException.providerRateLimited();
            }
            throw ChatbotException.providerUnavailable();
        } catch (RestClientException exception) {
            throw ChatbotException.providerUnavailable();
        }

        if (response == null) {
            throw ChatbotException.providerUnavailable();
        }
        return parseReply(response);
    }

    private ObjectNode createRequest(String instructions, String message, List<ChatbotHistoryTurn> history) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("model", properties.getModel());
        request.put("store", false);
        request.put("max_output_tokens", properties.getMaxOutputTokens());
        request.put("instructions", instructions);

        ArrayNode input = request.putArray("input");
        for (ChatbotHistoryTurn turn : history) {
            addInputTurn(input, turn.role(), turn.content());
        }
        addInputTurn(input, "user", message);

        ObjectNode format = request.putObject("text").putObject("format");
        format.put("type", "json_schema");
        format.put("name", RESPONSE_SCHEMA_NAME);
        format.put("strict", true);
        format.set("schema", responseSchema());
        return request;
    }

    private void addInputTurn(ArrayNode input, String role, String content) {
        ObjectNode turn = input.addObject();
        turn.put("role", role);
        turn.put("content", content);
    }

    private ObjectNode responseSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ArrayNode required = schema.putArray("required");
        required.add("reply").add("topic").add("status");

        ObjectNode propertiesNode = schema.putObject("properties");
        propertiesNode.putObject("reply").put("type", "string");

        ObjectNode topic = propertiesNode.putObject("topic");
        topic.put("type", "string");
        ArrayNode topics = topic.putArray("enum");
        for (ChatbotTopic value : ChatbotTopic.values()) {
            topics.add(value.name());
        }

        ObjectNode status = propertiesNode.putObject("status");
        status.put("type", "string");
        ArrayNode statuses = status.putArray("enum");
        for (ChatbotReplyStatus value : ChatbotReplyStatus.values()) {
            statuses.add(value.name());
        }
        return schema;
    }

    private ChatbotAiReply parseReply(JsonNode response) {
        String structuredReply = findOutputText(response);
        if (structuredReply == null || structuredReply.isBlank()) {
            throw ChatbotException.providerUnavailable();
        }
        try {
            ChatbotAiReply reply = objectMapper.readValue(structuredReply, ChatbotAiReply.class);
            if (reply.reply() == null || reply.reply().isBlank() || reply.reply().length() > 3000
                    || reply.topic() == null || reply.status() == null) {
                throw ChatbotException.providerUnavailable();
            }
            if (reply.status() == ChatbotReplyStatus.OUT_OF_SCOPE && reply.topic() != ChatbotTopic.UNKNOWN) {
                return new ChatbotAiReply(reply.reply(), ChatbotTopic.UNKNOWN, reply.status());
            }
            return reply;
        } catch (JsonProcessingException exception) {
            throw ChatbotException.providerUnavailable();
        }
    }

    private String findOutputText(JsonNode response) {
        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode item : output) {
            for (JsonNode content : item.path("content")) {
                if ("output_text".equals(content.path("type").asText())) {
                    text.append(content.path("text").asText());
                }
                if ("refusal".equals(content.path("type").asText())) {
                    return "{\"reply\":\"I can help with general LegacyVault product questions, but I can't assist with that request.\",\"topic\":\"UNKNOWN\",\"status\":\"OUT_OF_SCOPE\"}";
                }
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    private String endpointUrl() {
        return properties.getBaseUrl().replaceAll("/+$", "") + "/responses";
    }
}
