package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.PLOCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PLOCLOMappingRepository extends JpaRepository<PLOCLOMapping, Long> {
}
