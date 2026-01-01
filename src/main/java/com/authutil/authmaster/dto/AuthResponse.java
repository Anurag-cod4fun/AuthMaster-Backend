package com.authutil.authmaster.dto;

public record AuthResponse(String accessToken, String refreshToken, String username) {
}
