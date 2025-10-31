package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.AssessmentRepository;
import org.fyp.tmssep490be.services.AssessmentService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
}
