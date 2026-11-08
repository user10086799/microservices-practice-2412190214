package com.zjgsu.cxt.dto;

import jakarta.validation.constraints.NotNull;

public record DeductStockRequest(@NotNull(message = "下单数量必须大于 0") Integer quantity) {
}
