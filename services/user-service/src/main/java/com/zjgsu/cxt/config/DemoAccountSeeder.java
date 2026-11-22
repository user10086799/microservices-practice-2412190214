package com.zjgsu.cxt.config;

import com.zjgsu.cxt.model.ShopUser;
import com.zjgsu.cxt.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoAccountSeeder implements ApplicationRunner {

    static final String BUYER_USERNAME = "buyer";
    static final String ADMIN_USERNAME = "admin";
    private static final String BUYER_HASH = "$2b$10$rhsmmzZHhu0LygB0r3dTWuCaJxKoGaad.zvcHCYJEVf4G1w/JPpbm";
    private static final String ADMIN_HASH = "$2b$10$U0SGUGcpbCrXDBBuX3SCXuxvLeD4dg6ZOOXdlE68L1IT7q1cHmBgC";

    private final UserRepository userRepository;

    public DemoAccountSeeder(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        saveIfAbsent(BUYER_USERNAME, "买家", BUYER_HASH, "BUYER");
        saveIfAbsent(ADMIN_USERNAME, "管理员", ADMIN_HASH, "ADMIN");
    }

    private void saveIfAbsent(String username, String name, String passwordHash, String role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        ShopUser user = new ShopUser();
        user.setName(name);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        userRepository.save(user);
    }
}
