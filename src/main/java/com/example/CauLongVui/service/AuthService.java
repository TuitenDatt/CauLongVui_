package com.example.CauLongVui.service;

import com.example.CauLongVui.dto.AuthResponse;
import com.example.CauLongVui.dto.LoginRequest;
import com.example.CauLongVui.dto.RegisterRequest;
import com.example.CauLongVui.entity.User;
import com.example.CauLongVui.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Đăng ký tài khoản mới (mặc định role CUSTOMER)
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng: " + req.getEmail());
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(User.Role.CUSTOMER)
                .build();

        userRepository.save(user);
        return toResponse(user, "Đăng ký thành công! Chào mừng bạn đến với Cầu Lông Vui");
    }

    // ── Đăng nhập
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email hoặc mật khẩu không chính xác"));

        if (!user.getActive()) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
        }

        return toResponse(user, "Đăng nhập thành công! Xin chào, " + user.getFullName());
    }

    // ── Lấy thông tin user theo ID
    public AuthResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        return toResponse(user, null);
    }

    // ── Helper
    private AuthResponse toResponse(User user, String message) {
        return AuthResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .message(message)
                .build();
    }
}
