package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.LoginRequest;
import com.zjgsu.cxt.dto.LoginResponse;
import com.zjgsu.cxt.model.ShopUser;
import com.zjgsu.cxt.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        ShopUser user = userRepository.findByUsername(request.username().trim()).orElse(null);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        return new LoginResponse(jwtService.issue(user), "Bearer", user.getUsername(), user.getRole(), user.getId());
    }
}
