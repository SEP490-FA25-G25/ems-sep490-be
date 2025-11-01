package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.user.CreateUserRequest;
import org.fyp.tmssep490be.dtos.user.UserResponse;
import org.fyp.tmssep490be.entities.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * User account service interface
 */
public interface UserAccountService {

    /**
     * Create new user account (admin only)
     */
    UserResponse createUser(CreateUserRequest request);

    /**
     * Get user by ID
     */
    UserResponse getUserById(Long id);

    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);

    /**
     * Get all users with pagination
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Update user status
     */
    UserResponse updateUserStatus(Long id, String status);

    /**
     * Delete user (soft delete or revoke access)
     */
    void deleteUser(Long id);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}
