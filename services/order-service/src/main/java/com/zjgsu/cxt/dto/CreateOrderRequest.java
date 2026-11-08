package com.zjgsu.cxt.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull(message = "用户不能为空") Long userId,
        @NotNull(message = "商品不能为空") Long productId,
        @NotNull(message = "下单数量必须大于 0") Integer quantity) {
}
