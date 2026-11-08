package com.zjgsu.cxt.dto;

import com.zjgsu.cxt.model.Product;
import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String description, BigDecimal price, Integer stock) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock());
    }
}
