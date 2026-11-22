package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.cxt.dto.CreateUserRequest;
import com.zjgsu.cxt.dto.LoginRequest;
import com.zjgsu.cxt.dto.LoginResponse;
import com.zjgsu.cxt.dto.UserResponse;
import com.zjgsu.cxt.model.ShopUser;
import com.zjgsu.cxt.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserAccountServiceTests {

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createUserAndReadItBack() {
        UserResponse created = userAccountService.create(new CreateUserRequest("陈鑫涛"));

        UserResponse loaded = userAccountService.get(created.id());

        assertThat(loaded.name()).isEqualTo("陈鑫涛");
    }

    @Test
    void missingUserThrowsNotFound() {
        assertThatThrownBy(() -> userAccountService.get(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户不存在");
    }

    @Test
    void loginStoresPasswordHashAndReturnsRole() {
        ShopUser buyer = userRepository.findByUsername("buyer").orElseThrow();
        assertThat(buyer.getPasswordHash()).startsWith("$2");
        assertThat(buyer.getPasswordHash()).doesNotContain("buyer-week09");
        assertThat(passwordEncoder.matches("buyer-week09", buyer.getPasswordHash())).isTrue();

        LoginResponse response = authService.login(new LoginRequest("buyer", "buyer-week09"));

        assertThat(response.role()).isEqualTo("BUYER");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.token()).isNotBlank();
        assertThat(response.token()).doesNotContain("buyer-week09");
    }

    @Test
    void wrongPasswordDoesNotLogin() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("buyer", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    void buyerCannotListUsersAndAdminCan() {
        LoginResponse buyer = authService.login(new LoginRequest("buyer", "buyer-week09"));
        LoginResponse admin = authService.login(new LoginRequest("admin", "admin-week09"));

        assertThatThrownBy(() -> userAccountService.listForAdmin(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未登录");
        assertThatThrownBy(() -> userAccountService.listForAdmin("Bearer " + buyer.token()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("权限不足");
        assertThat(userAccountService.listForAdmin("Bearer " + admin.token()))
                .extracting(UserResponse::username)
                .contains("buyer", "admin");
    }
}
