package com.zjgsu.cxt.repository;

import com.zjgsu.cxt.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
