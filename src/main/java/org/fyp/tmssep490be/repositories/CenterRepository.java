package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Center;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CenterRepository extends JpaRepository<Center, Long> {

    boolean existsByCode(String code);

    Optional<Center> findByCode(String code);
}
