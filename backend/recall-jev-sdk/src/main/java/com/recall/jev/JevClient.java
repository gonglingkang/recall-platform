package com.recall.jev;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recall.jev.exception.JevApiException;
import com.recall.jev.exception.JevConnectionException;
import com.recall.jev.exception.JevException;
import com.recall.jev.exception.JevResponseValidationException;
import com.recall.jev.exception.JevTimeoutException;
import com.recall.jev.model.ListModelsResponse;
import com.recall.jev.model.Question;
import com.recall.jev.model.SystemOneRequest;
import com.recall.jev.model.SystemOneResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * jev 模型（TypeSafe AI System One）同步客户端，Java 移植自官方 typesafe-sdk-python。
 * <p>
 * 端点：POST /v1/systemone（结构化问答）、GET /v1/models（模型清单）；
 * 认证：Authorization Bearer；重试：见 {@link JevRetryPolicy}。
 * 通过 {@code recall.jev.enabled=true} 开关注册为 Spring Bean（见 JevAutoConfiguration）。
 *
 * @author recall
 */
public class JevClient implements AutoCloseable {

    public static final String SYSTEM_ONE_PATH = "/v1/systemone";
    public static final String MODELS_PATH = "/v1/models";

    private static final String SDK_NAME = "recall-jev-sdk";
    private static final String SDK_VERSION = "1.0";

    private final JevProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public JevClient(JevProperties props) {
        validateApiKey(props.getApiKey());
        this.props = props;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 结构化问答：对 state 提出一组命名问题，返回各题答案与用量。
     *
     * @param state     待评估内容：字符串、JSON 对象或数组
     * @param questions 问题集合，key=答案标识名，非空
     * @return 答案按问题名分组（nouls()/choices()/scores() 便捷访问）
     */
    public SystemOneResponse systemOne(Object state, Map<String, Question> questions) {
        return systemOne(state, questions, null);
    }

    /** 同上，model 覆盖客户端默认模型 */
    public SystemOneResponse systemOne(Object state, Map<String, Question> questions, String modelOverride) {
        if (questions == null || questions.isEmpty()) {
            throw new JevException("At least one question is required.");
        }
        questions.forEach(JevClient::validateQuestion);
        SystemOneRequest request = new SystemOneRequest();
        request.setState(state);
        request.setModel(modelOverride != null && !modelOverride.isBlank() ? modelOverride : props.getModel());
        request.setQuestions(questions);
        return send("POST", SYSTEM_ONE_PATH, request, SystemOneResponse.class);
    }

    /** 查询当前账号可用模型清单 */
    public ListModelsResponse listModels() {
        return send("GET", MODELS_PATH, null, ListModelsResponse.class);
    }

    /**
     * 释放客户端资源。
     * <p>
     * JDK 17 的 java.net.http.HttpClient 尚无 close()（21 起才有），
     * 其内部线程为守护线程不阻塞 JVM 退出，此处暂为空实现；升级 JDK 21 后可改为 http.close()。
     */
    @Override
    public void close() {
    }

    // ===================== 内部 =====================

    private static void validateQuestion(String name, Question question) {
        if (question == null || question.getType() == null || question.getType().isBlank()) {
            throw new JevException("Question \"" + name + "\" must have a nonempty type.");
        }
        if ((Question.TYPE_CHOICE.equals(question.getType()) || Question.TYPE_SCORE.equals(question.getType()))
                && question.getCriteria() == null) {
            throw new JevException("Question \"" + name + "\" requires criteria.");
        }
    }

    private static void validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new JevException("No API key was provided. Set recall.jev.api-key.");
        }
        if (apiKey.chars().anyMatch(ch -> ch <= 32 || ch > 126)) {
            throw new JevException("API key must contain only printable ASCII characters without whitespace.");
        }
    }

    /** 单次调用 + 重试循环：可重试异常在预算内按策略退避重试，否则抛出 */
    private <T> T send(String method, String path, Object body, Class<T> responseType) {
        long budgetEndNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(props.getRetryBudgetSeconds());
        String endpoint = method + " " + props.getBaseUrl() + path;
        byte[] content = encodeBody(body);
        int attempt = 0;
        while (true) {
            try {
                return attemptOnce(method, path, content, responseType, endpoint, attempt);
            } catch (JevException e) {
                attempt++;
                if (!JevRetryPolicy.isRetryable(e) || attempt > props.getMaxRetries()
                        || System.nanoTime() >= budgetEndNanos) {
                    throw e;
                }
                sleepQuietly(JevRetryPolicy.computeDelayMs(e, attempt - 1));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T attemptOnce(String method, String path, byte[] content, Class<T> responseType,
                              String endpoint, int attempt) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(props.getBaseUrl() + path))
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + props.getApiKey())
                .header("Accept", "application/json")
                .header("User-Agent", SDK_NAME + "/" + SDK_VERSION)
                .header("X-TypeSafe-SDK", SDK_NAME + "/" + SDK_VERSION)
                .header("X-TypeSafe-Runtime", "java/" + System.getProperty("java.version"));
        if (content != null) {
            builder.header("Content-Type", "application/json");
            builder.method(method, HttpRequest.BodyPublishers.ofByteArray(content));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        if (attempt > 0) {
            builder.header("X-TypeSafe-Retry-Count", String.valueOf(attempt));
        }
        HttpResponse<byte[]> response;
        try {
            response = http.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (HttpTimeoutException e) {
            throw new JevTimeoutException(props.getTimeoutSeconds() * 1000L);
        } catch (IOException e) {
            throw new JevConnectionException("Connection error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JevConnectionException("Request interrupted", e);
        }
        int status = response.statusCode();
        Map<String, String> headers = flattenHeaders(response);
        if (status >= 200 && status < 300) {
            return decode(response.body(), responseType, status, headers, endpoint);
        }
        throw new JevApiException(status, decodeBody(response.body()), headers, endpoint);
    }

    private <T> T decode(byte[] raw, Class<T> responseType, int status,
                         Map<String, String> headers, String endpoint) {
        try {
            return mapper.readValue(raw, responseType);
        } catch (IOException e) {
            Object body = decodeBody(raw);
            throw new JevResponseValidationException(status, body, headers, "", endpoint);
        }
    }

    private Object decodeBody(byte[] raw) {
        if (raw == null || raw.length == 0) {
            return null;
        }
        String text = new String(raw, StandardCharsets.UTF_8);
        try {
            return mapper.readValue(text, Object.class);
        } catch (IOException e) {
            return text;
        }
    }

    private byte[] encodeBody(Object body) {
        if (body == null) {
            return null;
        }
        try {
            return mapper.writeValueAsBytes(body);
        } catch (IOException e) {
            throw new JevException("The request body could not be encoded as JSON", e);
        }
    }

    private static Map<String, String> flattenHeaders(HttpResponse<byte[]> response) {
        Map<String, String> result = new HashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (!values.isEmpty()) {
                result.put(name.toLowerCase(), values.get(0));
            }
        });
        return result;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JevConnectionException("Retry interrupted", e);
        }
    }
}
