package com.zjgsu.cxt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zjgsu.cxt.dto.CreateUserRequest;
import com.zjgsu.cxt.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserAccountServiceTests {

    @Autowired
    private UserAccountService userAccountService;

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
}
