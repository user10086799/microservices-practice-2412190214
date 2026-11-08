package com.zjgsu.cxt.repository;

import com.zjgsu.cxt.model.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
}
