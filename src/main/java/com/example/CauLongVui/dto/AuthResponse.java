package com.example.CauLongVui.dto;

import com.example.CauLongVui.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private User.Role role;
    private String message;
}
