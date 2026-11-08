package com.zjgsu.cxt.dto;

import java.math.BigDecimal;

public record ProductSnapshot(Long id, String name, String description, BigDecimal price, Integer stock) {
}
