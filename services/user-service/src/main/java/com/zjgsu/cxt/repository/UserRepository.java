package com.zjgsu.cxt.repository;

import com.zjgsu.cxt.model.ShopUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<ShopUser, Long> {

    Optional<ShopUser> findByUsername(String username);
}
