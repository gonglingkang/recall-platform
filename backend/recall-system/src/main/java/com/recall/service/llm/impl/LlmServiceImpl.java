package com.recall.service.llm.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recall.common.api.ResultCode;
import com.recall.common.exception.BusinessException;
import com.recall.service.llm.LlmService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 大模型对话 Service 实现（Anthropic Messages 协议）。
 * <p>
 * 网关在禁用 thinking 时直接输出 text 块；解析时仍只取 text 块，
 * 防御网关侧模型行为变化导致 thinking 块混入。
 *
 * @author recall
 */
@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    @Value("${recall.llm.base-url}")
    private String baseUrl;

    @Value("${recall.llm.api-key}")
    private String apiKey;

    @Value("${recall.llm.model}")
    private String model;

    @Value("${recall.llm.timeout-seconds:60}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RestClient restClient;

    @PostConstruct
    void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .requestFactory(factory)
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        // thinking 显式禁用：思考块会消耗输出 token 且拖慢响应
        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 4000,
                "thinking", Map.of("type", "disabled"),
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)));
        String resp;
        try {
            resp = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("大模型调用失败: model={}, err={}", model, e.getMessage());
            throw new BusinessException(ResultCode.LLM_CALL_FAILED);
        }
        return extractText(resp);
    }

    /** 拼接 content 数组中的 text 块；为空视为返回内容异常 */
    private String extractText(String resp) {
        try {
            StringBuilder sb = new StringBuilder();
            JsonNode content = objectMapper.readTree(resp).path("content");
            if (content.isArray()) {
                content.forEach(node -> {
                    if ("text".equals(node.path("type").asText())) {
                        sb.append(node.path("text").asText());
                    }
                });
            }
            String text = sb.toString().trim();
            if (text.isEmpty()) {
                throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
            }
            return text;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("大模型响应解析失败: err={}", e.getMessage());
            throw new BusinessException(ResultCode.LLM_RESPONSE_INVALID);
        }
    }
}
