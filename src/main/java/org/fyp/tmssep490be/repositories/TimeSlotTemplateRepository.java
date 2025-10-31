package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TimeSlotTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TimeSlotTemplateRepository extends JpaRepository<TimeSlotTemplate, Long> {
}
