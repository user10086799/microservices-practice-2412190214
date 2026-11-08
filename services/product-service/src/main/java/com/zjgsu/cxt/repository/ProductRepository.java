package com.zjgsu.cxt.repository;

import com.zjgsu.cxt.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
