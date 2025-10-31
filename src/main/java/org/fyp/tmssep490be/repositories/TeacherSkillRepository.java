package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeacherSkillRepository extends JpaRepository<TeacherSkill, Long> {
}
