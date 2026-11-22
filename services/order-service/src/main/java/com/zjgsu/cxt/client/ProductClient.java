package com.zjgsu.cxt.client;

import com.zjgsu.cxt.dto.ProductSnapshot;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ResponseEntity<ProductSnapshot> getProduct(@PathVariable("id") Long id);

    @PostMapping("/api/products/{id}/stock-deductions")
    ProductSnapshot deductStock(@PathVariable("id") Long id, @RequestBody Map<String, Integer> body);
}
