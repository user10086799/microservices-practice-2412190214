package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.cxt.dto.CreateProductRequest;
import com.zjgsu.cxt.dto.ProductResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductCatalogServiceTests {

    @Autowired
    private ProductCatalogService productCatalogService;

    @Test
    void deductStockDecreasesRemainingQuantity() {
        ProductResponse created = productCatalogService.create(
                new CreateProductRequest("无线鼠标", "静音办公鼠标", new BigDecimal("89.00"), 20));

        ProductResponse updated = productCatalogService.deductStock(created.id(), 1);

        assertThat(updated.stock()).isEqualTo(19);
        assertThat(updated.price()).isEqualByComparingTo("89.00");
    }

    @Test
    void rejectDeductionWhenQuantityExceedsStock() {
        ProductResponse created = productCatalogService.create(
                new CreateProductRequest("机械键盘", "87 键热插拔键盘", new BigDecimal("299.00"), 10));

        assertThatThrownBy(() -> productCatalogService.deductStock(created.id(), 11))
                .isInstanceOf(BusinessException.class)
                .hasMessage("下单数量不能超过商品库存");
        assertThat(productCatalogService.get(created.id()).stock()).isEqualTo(10);
    }
}
