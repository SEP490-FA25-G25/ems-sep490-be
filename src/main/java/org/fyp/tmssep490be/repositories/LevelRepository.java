package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LevelRepository extends JpaRepository<Level, Long> {

    /**
     * Find level by code (case-insensitive)
     */
    java.util.Optional<Level> findByCodeIgnoreCase(String code);
}
