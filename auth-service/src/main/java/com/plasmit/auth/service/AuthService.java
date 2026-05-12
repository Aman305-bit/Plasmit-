package com.plasmit.auth.service;

import com.plasmit.auth.dto.request.LoginRequest;
import com.plasmit.auth.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    LoginResponse.AuthUserResponse getCurrentUser(Long userId);
}