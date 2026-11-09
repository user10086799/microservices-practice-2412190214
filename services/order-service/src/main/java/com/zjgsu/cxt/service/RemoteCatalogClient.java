package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.ProductSnapshot;
import com.zjgsu.cxt.dto.UserProfile;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class RemoteCatalogClient {

    static final String USER_SERVICE = "user-service";
    static final String PRODUCT_SERVICE = "product-service";

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;

    public RemoteCatalogClient(RestTemplate restTemplate, DiscoveryClient discoveryClient) {
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
    }

    public UserProfile requireUser(Long userId) {
        String url = instanceBase(USER_SERVICE, "用户服务不可用") + "/api/users/" + userId;
        try {
            UserProfile user = restTemplate.getForObject(url, UserProfile.class);
            if (user == null || user.id() == null) {
                throw new BusinessException(404, "用户不存在");
            }
            return user;
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(exception.getStatusCode().value(), messageOf(exception, "用户不存在"));
        } catch (ResourceAccessException exception) {
            throw new BusinessException(503, "用户服务不可用");
        }
    }

    public ProductSnapshot deductStock(Long productId, int quantity) {
        String url = instanceBase(PRODUCT_SERVICE, "商品服务不可用") + "/api/products/" + productId + "/stock-deductions";
        try {
            ProductSnapshot product = restTemplate.postForObject(url, Map.of("quantity", quantity), ProductSnapshot.class);
            if (product == null || product.id() == null || product.price() == null) {
                throw new BusinessException(502, "商品服务返回了无效数据");
            }
            return product;
        } catch (BusinessException exception) {
            throw exception;
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(exception.getStatusCode().value(), messageOf(exception, "商品服务拒绝请求"));
        } catch (ResourceAccessException exception) {
            throw new BusinessException(503, "商品服务不可用");
        }
    }

    private String instanceBase(String serviceName, String unavailableMessage) {
        List<ServiceInstance> instances;
        try {
            instances = discoveryClient.getInstances(serviceName);
        } catch (RuntimeException exception) {
            throw new BusinessException(503, unavailableMessage);
        }
        if (instances == null || instances.isEmpty() || instances.get(0).getUri() == null) {
            throw new BusinessException(503, unavailableMessage);
        }
        return instances.get(0).getUri().toString();
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
