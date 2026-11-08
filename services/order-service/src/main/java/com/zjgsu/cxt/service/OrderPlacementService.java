package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.CreateOrderRequest;
import com.zjgsu.cxt.dto.OrderResponse;
import com.zjgsu.cxt.dto.ProductSnapshot;
import com.zjgsu.cxt.dto.UserProfile;
import com.zjgsu.cxt.model.CustomerOrder;
import com.zjgsu.cxt.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPlacementService {

    private final OrderRepository orderRepository;
    private final RemoteCatalogClient remoteCatalogClient;

    public OrderPlacementService(OrderRepository orderRepository, RemoteCatalogClient remoteCatalogClient) {
        this.orderRepository = orderRepository;
        this.remoteCatalogClient = remoteCatalogClient;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BusinessException(400, "下单数量必须大于 0");
        }
        UserProfile user = remoteCatalogClient.requireUser(request.userId());
        ProductSnapshot product = remoteCatalogClient.deductStock(request.productId(), request.quantity());

        CustomerOrder order = new CustomerOrder();
        order.setUserId(user.id());
        order.setBuyerName(user.name());
        order.setProductId(product.id());
        order.setProductName(product.name());
        order.setQuantity(request.quantity());
        order.setAmount(product.price().multiply(BigDecimal.valueOf(request.quantity())));
        order.setStatus(CustomerOrder.PENDING_PAYMENT);
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
