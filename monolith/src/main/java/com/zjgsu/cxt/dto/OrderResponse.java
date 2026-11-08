package com.zjgsu.cxt.dto;

import com.zjgsu.cxt.model.Order;
import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        String buyerName,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal amount,
        String status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getBuyerName(),
                order.getProduct().getId(),
                order.getProduct().getName(),
                order.getQuantity(),
                order.getAmount(),
                order.getStatus());
    }
}
