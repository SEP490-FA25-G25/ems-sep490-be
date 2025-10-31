package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.ReplacementSkillAssessmentRepository;
import org.fyp.tmssep490be.services.ReplacementSkillAssessmentService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReplacementSkillAssessmentServiceImpl implements ReplacementSkillAssessmentService {

    private final ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;
}
