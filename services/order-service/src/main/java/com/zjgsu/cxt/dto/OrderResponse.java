package com.zjgsu.cxt.dto;

import com.zjgsu.cxt.model.CustomerOrder;
import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long userId,
        String buyerName,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String status) {

    public static OrderResponse from(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getBuyerName(),
                order.getProductId(),
                order.getProductName(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus());
    }
}
