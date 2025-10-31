package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {
}
