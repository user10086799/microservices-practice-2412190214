package com.zjgsu.cxt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotBlank String buyerName,
        @NotNull Long productId,
        @NotNull Integer quantity) {
}
