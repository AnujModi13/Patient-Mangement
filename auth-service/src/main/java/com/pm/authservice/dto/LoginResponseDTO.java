package com.pm.authservice.dto;

public class LoginResponseDTO {
    public String getToken() {
        return token;
    }

    private final String token;

    public LoginResponseDTO(String token) {
        this.token = token;
    }
}
