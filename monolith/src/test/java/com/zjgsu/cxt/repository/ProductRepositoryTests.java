package com.zjgsu.cxt.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.zjgsu.cxt.model.Product;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductRepositoryTests {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saveAndReadProduct() {
        Product product = new Product();
        product.setName("测试商品");
        product.setDescription("仓储读写");
        product.setPrice(new BigDecimal("12.50"));
        product.setStock(3);

        Product saved = productRepository.save(product);

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("测试商品");
        assertThat(found.getPrice()).isEqualByComparingTo("12.50");
        assertThat(found.getStock()).isEqualTo(3);
    }
}
