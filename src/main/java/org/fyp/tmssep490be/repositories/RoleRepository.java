package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    /**
     * Find role by code (e.g., "STUDENT", "TEACHER", "ADMIN")
     */
    Optional<Role> findByCode(String code);
}
