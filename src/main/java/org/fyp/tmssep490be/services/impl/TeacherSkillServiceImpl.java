package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.TeacherSkillRepository;
import org.fyp.tmssep490be.services.TeacherSkillService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherSkillServiceImpl implements TeacherSkillService {

    private final TeacherSkillRepository teacherSkillRepository;
}
