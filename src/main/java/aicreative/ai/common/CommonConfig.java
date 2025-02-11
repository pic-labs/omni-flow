package aicreative.ai.common;

import aicreative.ai.common.oss.LocalOssHandler;
import aicreative.ai.common.oss.OssHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonConfig {

    @Bean
    @ConditionalOnProperty(name = "common.oss.type", havingValue = "local", matchIfMissing = true)
    public OssHandler localOssHandler(ObjectMapper objectMapper) {
        return new LocalOssHandler(objectMapper);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
