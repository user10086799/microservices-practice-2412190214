package com.zjgsu.cxt.repository;

import com.zjgsu.cxt.model.ShopUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<ShopUser, Long> {
}
