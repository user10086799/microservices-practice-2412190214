package com.zjgsu.cxt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "商品名称不能为空") String name,
        String description,
        @NotNull(message = "商品价格不能为空") BigDecimal price,
        @NotNull(message = "商品库存不能为空") Integer stock) {
}
