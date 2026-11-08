package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.cxt.dto.CreateOrderRequest;
import com.zjgsu.cxt.dto.OrderResponse;
import com.zjgsu.cxt.model.Order;
import com.zjgsu.cxt.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderServiceTests {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void createOrderDeductsStockAndCalculatesAmount() {
        Long productId = productRepository.findByName("机械键盘").orElseThrow().getId();

        OrderResponse order = orderService.create(new CreateOrderRequest("陈鑫涛", productId, 2));

        assertThat(order.status()).isEqualTo(Order.PENDING_PAYMENT);
        assertThat(order.amount()).isEqualByComparingTo("598.00");
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(8);
    }

    @Test
    void rejectOrderWhenQuantityExceedsStock() {
        Long productId = productRepository.findByName("机械键盘").orElseThrow().getId();

        assertThatThrownBy(() -> orderService.create(new CreateOrderRequest("陈鑫涛", productId, 11)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("下单数量不能超过商品库存");
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(10);
    }
}
