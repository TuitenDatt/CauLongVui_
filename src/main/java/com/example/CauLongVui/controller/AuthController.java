package com.example.CauLongVui.controller;

import com.example.CauLongVui.dto.ApiResponse;
import com.example.CauLongVui.dto.AuthResponse;
import com.example.CauLongVui.dto.LoginRequest;
import com.example.CauLongVui.dto.RegisterRequest;
import com.example.CauLongVui.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register — Đăng ký tài khoản mới
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.getMessage(), response));
    }

    // POST /api/auth/login — Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    // GET /api/auth/me?id={id} — Lấy thông tin user (dùng khi reload trang)
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthResponse>> getMe(@RequestParam Long id) {
        AuthResponse response = authService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
