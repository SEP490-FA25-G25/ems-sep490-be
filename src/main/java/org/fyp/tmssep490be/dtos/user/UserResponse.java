package org.fyp.tmssep490be.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;

import java.time.LocalDate;
import java.util.Set;

/**
 * User account response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private String facebookUrl;
    private LocalDate dob;
    private Gender gender;
    private String address;
    private UserStatus status;
    private Set<String> roles;
    private Set<String> branches;
}
