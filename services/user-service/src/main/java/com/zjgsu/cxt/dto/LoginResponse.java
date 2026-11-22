package com.zjgsu.cxt.dto;

public record LoginResponse(String token, String tokenType, String username, String role, Long userId) {
}
