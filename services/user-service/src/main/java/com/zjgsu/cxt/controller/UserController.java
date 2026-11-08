package com.zjgsu.cxt.controller;

import com.zjgsu.cxt.dto.CreateUserRequest;
import com.zjgsu.cxt.dto.UserResponse;
import com.zjgsu.cxt.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAccountService userAccountService;

    public UserController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userAccountService.create(request);
    }

    @GetMapping
    public List<UserResponse> list() {
        return userAccountService.list();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userAccountService.get(id);
    }
}
