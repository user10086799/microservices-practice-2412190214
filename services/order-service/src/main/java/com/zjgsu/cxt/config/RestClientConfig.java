package com.zjgsu.cxt.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

    @Bean
    RestTemplate restTemplate(ClientProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(orDefault(properties.connectTimeout(), Duration.ofSeconds(2)));
        factory.setReadTimeout(orDefault(properties.readTimeout(), Duration.ofSeconds(3)));
        return new RestTemplate(factory);
    }

    private static Duration orDefault(Duration value, Duration fallback) {
        return value == null ? fallback : value;
    }
}
