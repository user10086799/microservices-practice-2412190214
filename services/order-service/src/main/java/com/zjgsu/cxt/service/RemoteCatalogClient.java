package com.zjgsu.cxt.service;

import com.zjgsu.cxt.config.ClientProperties;
import com.zjgsu.cxt.dto.ProductSnapshot;
import com.zjgsu.cxt.dto.UserProfile;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class RemoteCatalogClient {

    private final RestTemplate restTemplate;
    private final ClientProperties properties;

    public RemoteCatalogClient(RestTemplate restTemplate, ClientProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public UserProfile requireUser(Long userId) {
        String url = properties.userServiceUrl() + "/api/users/" + userId;
        try {
            UserProfile user = restTemplate.getForObject(url, UserProfile.class);
            if (user == null || user.id() == null) {
                throw new BusinessException(404, "用户不存在");
            }
            return user;
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(exception.getStatusCode().value(), messageOf(exception, "用户不存在"));
        } catch (ResourceAccessException exception) {
            throw new BusinessException(503, "用户服务不可用");
        }
    }

    public ProductSnapshot deductStock(Long productId, int quantity) {
        String url = properties.productServiceUrl() + "/api/products/" + productId + "/stock-deductions";
        try {
            ProductSnapshot product = restTemplate.postForObject(url, Map.of("quantity", quantity), ProductSnapshot.class);
            if (product == null || product.id() == null || product.price() == null) {
                throw new BusinessException(502, "商品服务返回了无效数据");
            }
            return product;
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(exception.getStatusCode().value(), messageOf(exception, "商品服务拒绝请求"));
        } catch (ResourceAccessException exception) {
            throw new BusinessException(503, "商品服务不可用");
        }
    }

    private static String messageOf(HttpStatusCodeException exception, String fallback) {
        String body = exception.getResponseBodyAsString();
        int key = body.indexOf("\"message\"");
        if (key < 0) {
            return fallback;
        }
        int colon = body.indexOf(':', key);
        int start = body.indexOf('"', colon + 1);
        int end = start < 0 ? -1 : body.indexOf('"', start + 1);
        if (start < 0 || end < 0) {
            return fallback;
        }
        String message = body.substring(start + 1, end);
        return message.isBlank() ? fallback : message;
    }
}
