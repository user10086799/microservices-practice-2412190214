package com.zjgsu.cxt.service;

import com.zjgsu.cxt.client.ProductClient;
import com.zjgsu.cxt.client.UserClient;
import com.zjgsu.cxt.dto.ProductProbe;
import com.zjgsu.cxt.dto.ProductSnapshot;
import com.zjgsu.cxt.dto.UserProfile;
import feign.FeignException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class RemoteCatalogClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteCatalogClient.class);

    private final UserClient userClient;
    private final ProductClient productClient;

    public RemoteCatalogClient(UserClient userClient, ProductClient productClient) {
        this.userClient = userClient;
        this.productClient = productClient;
    }

    public UserProfile requireUser(Long userId) {
        log.info("OpenFeign 调用服务名 user-service GET /api/users/{}", userId);
        try {
            UserProfile user = userClient.getUser(userId);
            if (user == null || user.id() == null) {
                throw new BusinessException(404, "用户不存在");
            }
            return user;
        } catch (BusinessException exception) {
            throw exception;
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new BusinessException(404, messageOf(exception, "用户不存在"));
            }
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new BusinessException(exception.status(), messageOf(exception, "用户服务拒绝请求"));
            }
            throw new BusinessException(503, "用户服务不可用");
        }
    }

    public ProductSnapshot deductStock(Long productId, int quantity) {
        log.info("OpenFeign 调用服务名 product-service POST /api/products/{}/stock-deductions", productId);
        try {
            ProductSnapshot product = productClient.deductStock(productId, Map.of("quantity", quantity));
            if (product == null || product.id() == null || product.price() == null) {
                throw new BusinessException(502, "商品服务返回了无效数据");
            }
            return product;
        } catch (BusinessException exception) {
            throw exception;
        } catch (FeignException exception) {
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new BusinessException(exception.status(), messageOf(exception, "商品服务拒绝请求"));
            }
            throw new BusinessException(503, "商品服务不可用");
        }
    }

    public ProductProbe probeProduct(Long productId) {
        log.info("OpenFeign 调用服务名 product-service GET /api/products/{}", productId);
        try {
            ResponseEntity<ProductSnapshot> response = productClient.getProduct(productId);
            ProductSnapshot product = response.getBody();
            if (product == null || product.id() == null) {
                throw new BusinessException(502, "商品服务返回了无效数据");
            }
            return new ProductProbe(response.getHeaders().getFirst("X-Instance-Id"), product.id(), product.name());
        } catch (BusinessException exception) {
            throw exception;
        } catch (FeignException exception) {
            if (exception.status() == 404) {
                throw new BusinessException(404, messageOf(exception, "商品不存在"));
            }
            if (exception.status() >= 400 && exception.status() < 500) {
                throw new BusinessException(exception.status(), messageOf(exception, "商品服务拒绝请求"));
            }
            throw new BusinessException(503, "商品服务不可用");
        }
    }

    private static String messageOf(FeignException exception, String fallback) {
        String body;
        try {
            body = exception.contentUTF8();
        } catch (RuntimeException ignored) {
            return fallback;
        }
        if (body == null) {
            return fallback;
        }
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
