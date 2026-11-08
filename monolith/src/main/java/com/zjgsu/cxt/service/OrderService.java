package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.CreateOrderRequest;
import com.zjgsu.cxt.dto.OrderResponse;
import com.zjgsu.cxt.model.Order;
import com.zjgsu.cxt.model.Product;
import com.zjgsu.cxt.repository.OrderRepository;
import com.zjgsu.cxt.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException(400, "下单数量必须大于 0");
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (request.quantity() > product.getStock()) {
            throw new BusinessException(400, "下单数量不能超过商品库存");
        }

        product.setStock(product.getStock() - request.quantity());

        Order order = new Order();
        order.setBuyerName(request.buyerName());
        order.setProduct(product);
        order.setQuantity(request.quantity());
        order.setAmount(product.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(Order.PENDING_PAYMENT);
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> list() {
        return orderRepository.findAll(Sort.by("id")).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }
}
