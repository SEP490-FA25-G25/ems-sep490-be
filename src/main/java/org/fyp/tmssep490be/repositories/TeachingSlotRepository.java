package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, Long> {
}
