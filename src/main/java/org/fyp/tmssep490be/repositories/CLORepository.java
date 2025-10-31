package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CLO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CLORepository extends JpaRepository<CLO, Long> {
}
