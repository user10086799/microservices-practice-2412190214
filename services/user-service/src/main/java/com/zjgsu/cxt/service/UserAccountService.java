package com.zjgsu.cxt.service;

import com.zjgsu.cxt.dto.CreateUserRequest;
import com.zjgsu.cxt.dto.UserResponse;
import com.zjgsu.cxt.model.ShopUser;
import com.zjgsu.cxt.repository.UserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccountService {

    private final UserRepository userRepository;

    public UserAccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        ShopUser user = new ShopUser();
        user.setName(request.name().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll(Sort.by("id")).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }
}
