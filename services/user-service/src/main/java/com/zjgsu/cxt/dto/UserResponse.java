package com.zjgsu.cxt.dto;

import com.zjgsu.cxt.model.ShopUser;

public record UserResponse(Long id, String name) {

    public static UserResponse from(ShopUser user) {
        return new UserResponse(user.getId(), user.getName());
    }
}
