package com.zjgsu.cxt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clients")
public record ClientProperties(Duration connectTimeout, Duration readTimeout) {
}
