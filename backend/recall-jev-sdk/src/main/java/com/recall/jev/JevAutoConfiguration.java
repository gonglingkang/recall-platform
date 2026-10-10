package com.recall.jev;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * jev 客户端自动装配。
 * <p>
 * 仅当 {@code recall.jev.enabled=true} 时注册 {@link JevClient} Bean；
 * 关闭（默认）时完全不装配，平台继续走现有 LLM 网关（recall.llm.*）。
 * 主应用组件扫描覆盖 com.recall.**，本配置类随扫描自动生效。
 *
 * @author recall
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "recall.jev", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(JevProperties.class)
public class JevAutoConfiguration {

    @Bean(destroyMethod = "close")
    public JevClient jevClient(JevProperties properties) {
        return new JevClient(properties);
    }
}
