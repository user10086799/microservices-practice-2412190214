package com.zjgsu.cxt.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(@NotBlank(message = "用户姓名不能为空") String name) {
}
