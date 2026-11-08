package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.CreateProductRequest;
import com.zjgsu.cxt.dto.ProductResponse;
import com.zjgsu.cxt.model.Product;
import com.zjgsu.cxt.repository.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (request.price().signum() < 0) {
            throw new BusinessException(400, "商品价格不能小于 0");
        }
        if (request.stock() < 0) {
            throw new BusinessException(400, "商品库存不能小于 0");
        }
        Product product = new Product();
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return productRepository.findAll(Sort.by("id")).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(requireProduct(id));
    }

    @Transactional
    public ProductResponse deductStock(Long id, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(400, "下单数量必须大于 0");
        }
        Product product = requireProduct(id);
        if (quantity > product.getStock()) {
            throw new BusinessException(400, "下单数量不能超过商品库存");
        }
        product.setStock(product.getStock() - quantity);
        return ProductResponse.from(productRepository.save(product));
    }

    private Product requireProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
    }
}
