package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {
}
