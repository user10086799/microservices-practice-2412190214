package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zjgsu.cxt.client.ProductClient;
import com.zjgsu.cxt.client.UserClient;
import com.zjgsu.cxt.dto.CreateOrderRequest;
import com.zjgsu.cxt.dto.OrderResponse;
import com.zjgsu.cxt.dto.ProductProbe;
import com.zjgsu.cxt.dto.ProductSnapshot;
import com.zjgsu.cxt.dto.UserProfile;
import com.zjgsu.cxt.model.CustomerOrder;
import com.zjgsu.cxt.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderPlacementServiceTests {

    @Autowired
    private OrderPlacementService orderPlacementService;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private UserClient userClient;

    @MockitoBean
    private ProductClient productClient;

    @Test
    void createOrderUsesFeignServiceNames() {
        when(userClient.getUser(1L)).thenReturn(new UserProfile(1L, "陈鑫涛"));
        when(productClient.deductStock(eq(2L), eq(Map.of("quantity", 1))))
                .thenReturn(new ProductSnapshot(2L, "无线鼠标", "静音办公鼠标", new BigDecimal("89.00"), 19));

        OrderResponse order = orderPlacementService.create(new CreateOrderRequest(1L, 2L, 1));

        assertThat(order.buyerName()).isEqualTo("陈鑫涛");
        assertThat(order.productName()).isEqualTo("无线鼠标");
        assertThat(order.amount()).isEqualByComparingTo("89.00");
        assertThat(order.status()).isEqualTo(CustomerOrder.PENDING_PAYMENT);
        verify(userClient).getUser(1L);
        verify(productClient).deductStock(2L, Map.of("quantity", 1));
        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void missingUserDoesNotCreateOrder() {
        when(userClient.getUser(9L)).thenThrow(feignStatus("UserClient#getUser", 404, "{\"message\":\"用户不存在\"}"));

        assertThatThrownBy(() -> orderPlacementService.create(new CreateOrderRequest(9L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void unavailableProductServiceDoesNotCreateOrder() {
        when(userClient.getUser(1L)).thenReturn(new UserProfile(1L, "陈鑫涛"));
        when(productClient.deductStock(eq(2L), any())).thenThrow(timeout());

        assertThatThrownBy(() -> orderPlacementService.create(new CreateOrderRequest(1L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("商品服务不可用");
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void probeReadsCalledInstanceId() {
        when(productClient.getProduct(2L)).thenReturn(ResponseEntity.ok()
                .header("X-Instance-Id", "product-b")
                .body(new ProductSnapshot(2L, "无线鼠标", "静音办公鼠标", new BigDecimal("89.00"), 19)));

        ProductProbe probe = orderPlacementService.probeProduct(2L);

        assertThat(probe.instanceId()).isEqualTo("product-b");
        assertThat(probe.productId()).isEqualTo(2L);
        assertThat(probe.name()).isEqualTo("无线鼠标");
    }

    private static FeignException feignStatus(String methodKey, int status, String body) {
        Request request = Request.create(
                Request.HttpMethod.GET,
                "/api/users/9",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        Response response = Response.builder()
                .status(status)
                .reason("error")
                .request(request)
                .body(body, StandardCharsets.UTF_8)
                .build();
        return FeignException.errorStatus(methodKey, response);
    }

    private static RetryableException timeout() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/api/products/2/stock-deductions",
                Map.of(),
                null,
                StandardCharsets.UTF_8,
                null);
        return new RetryableException(-1, "Read timed out", Request.HttpMethod.POST, (Long) null, request);
    }
}
