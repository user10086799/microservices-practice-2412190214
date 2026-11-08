package com.zjgsu.cxt.controller;

import com.zjgsu.cxt.dto.CreateProductRequest;
import com.zjgsu.cxt.dto.DeductStockRequest;
import com.zjgsu.cxt.dto.ProductResponse;
import com.zjgsu.cxt.service.ProductCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductCatalogService productCatalogService;

    public ProductController(ProductCatalogService productCatalogService) {
        this.productCatalogService = productCatalogService;
    }

    @PostMapping
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productCatalogService.create(request);
    }

    @GetMapping
    public List<ProductResponse> list() {
        return productCatalogService.list();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productCatalogService.get(id);
    }

    @PostMapping("/{id}/stock-deductions")
    public ProductResponse deductStock(@PathVariable Long id, @Valid @RequestBody DeductStockRequest request) {
        return productCatalogService.deductStock(id, request.quantity());
    }
}
