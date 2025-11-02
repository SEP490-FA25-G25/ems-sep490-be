package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    /**
     * Find student by student_code
     */
    Optional<Student> findByStudentCode(String studentCode);

    /**
     * Find student by user_id
     */
    Optional<Student> findByUserAccountId(Long userId);
}
