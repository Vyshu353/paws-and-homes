package com.pawsandhomes.petadoption.auth.dto;

import com.pawsandhomes.petadoption.entity.User;
import com.pawsandhomes.petadoption.entity.UserRole;

public record UserResponse(Long id, String fullName, String name, String email, String phoneNumber, UserRole role) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole());
    }
}
