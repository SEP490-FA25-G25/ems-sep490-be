package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {

    /**
     * Find all time slots for a specific branch, ordered by start time
     */
    List<TimeSlotTemplate> findByBranchIdOrderByStartTimeAsc(Long branchId);
}
