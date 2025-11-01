package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;

/**
 * Authentication service interface - simplified without database-stored refresh tokens
 */
public interface AuthService {

    /**
     * Authenticate user and generate tokens
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refresh access token using refresh token (validates JWT only, no DB lookup)
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
}
