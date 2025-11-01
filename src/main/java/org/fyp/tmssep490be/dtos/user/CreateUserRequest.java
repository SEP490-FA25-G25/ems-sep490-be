package org.fyp.tmssep490be.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * Create user account request DTO (admin only)
 * Note: Email is used as the login identifier (no separate username)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String facebookUrl;

    private LocalDate dob;

    @NotNull(message = "Gender is required")
    private Gender gender = Gender.MALE;

    private String address;

    @NotNull(message = "Status is required")
    private UserStatus status = UserStatus.ACTIVE;

    @NotNull(message = "At least one role is required")
    @Size(min = 1, message = "At least one role is required")
    private Set<Long> roleIds;

    private Set<Long> branchIds;
}
