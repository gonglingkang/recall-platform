package com.recall.jev;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jev 模型（TypeSafe System One）客户端配置。
 * <p>
 * {@code recall.jev.enabled=false}（默认）时不装配 JevClient，平台继续走现有 LLM 网关；
 * 置 true 需同时提供 api-key，否则启动失败。
 *
 * @author recall
 */
@Data
@ConfigurationProperties(prefix = "recall.jev")
public class JevProperties {

    /** 是否启用 jev 客户端（开关） */
    private boolean enabled = false;

    /** API Key（Bearer），形如 TYPESAFE_API_KEY */
    private String apiKey = "";

    /** API 根地址，可指向代理/内网网关 */
    private String baseUrl = "https://api.typesafe.ai";

    /** 默认模型，如 jev-latest */
    private String model = "jev-latest";

    /** 单次 HTTP 操作超时秒数 */
    private int timeoutSeconds = 10;

    /** 最大重试次数（不含首次请求） */
    private int maxRetries = 2;

    /** 单次调用（含重试与等待）总预算秒数，超预算不再重试 */
    private int retryBudgetSeconds = 30;
}
