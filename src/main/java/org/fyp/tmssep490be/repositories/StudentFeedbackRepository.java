package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentFeedbackRepository extends JpaRepository<StudentFeedback, Long> {
}
