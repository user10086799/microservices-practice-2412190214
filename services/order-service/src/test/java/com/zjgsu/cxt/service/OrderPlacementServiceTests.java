package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.zjgsu.cxt.dto.CreateOrderRequest;
import com.zjgsu.cxt.dto.OrderResponse;
import com.zjgsu.cxt.model.CustomerOrder;
import com.zjgsu.cxt.repository.OrderRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@Transactional
class OrderPlacementServiceTests {

    @Autowired
    private OrderPlacementService orderPlacementService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    @MockitoBean
    private DiscoveryClient discoveryClient;

    @BeforeEach
    void resetRestTemplate() {
        restTemplate.setInterceptors(new ArrayList<>());
        restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory());
        ServiceInstance user = instance("http://user.test");
        ServiceInstance product = instance("http://product.test");
        when(discoveryClient.getInstances("user-service")).thenReturn(List.of(user));
        when(discoveryClient.getInstances("product-service")).thenReturn(List.of(product));
    }

    @Test
    void createOrderUsesDiscoveredUserAndProduct() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://user.test/api/users/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"陈鑫涛\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://product.test/api/products/2/stock-deductions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"id\":2,\"name\":\"无线鼠标\",\"description\":\"静音办公鼠标\",\"price\":89.00,\"stock\":19}",
                        MediaType.APPLICATION_JSON));

        OrderResponse order = orderPlacementService.create(new CreateOrderRequest(1L, 2L, 1));

        assertThat(order.buyerName()).isEqualTo("陈鑫涛");
        assertThat(order.productName()).isEqualTo("无线鼠标");
        assertThat(order.amount()).isEqualByComparingTo("89.00");
        assertThat(order.status()).isEqualTo(CustomerOrder.PENDING_PAYMENT);
        server.verify();
    }

    @Test
    void missingUserDoesNotCreateOrder() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://user.test/api/users/9"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"用户不存在\"}"));

        assertThatThrownBy(() -> orderPlacementService.create(new CreateOrderRequest(9L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void emptyInstanceListDoesNotCreateOrder() {
        when(discoveryClient.getInstances("user-service")).thenReturn(List.of());

        assertThatThrownBy(() -> orderPlacementService.create(new CreateOrderRequest(1L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户服务不可用");
        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void unavailableProductServiceDoesNotCreateOrder() {
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://user.test/api/users/1"))
                .andRespond(withSuccess("{\"id\":1,\"name\":\"陈鑫涛\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://product.test/api/products/2/stock-deductions"))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection refused");
                });

        assertThatThrownBy(() -> orderPlacementService.create(new CreateOrderRequest(1L, 2L, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("商品服务不可用");
        assertThat(orderRepository.count()).isZero();
    }

    private static ServiceInstance instance(String uri) {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getUri()).thenReturn(URI.create(uri));
        return instance;
    }
}
