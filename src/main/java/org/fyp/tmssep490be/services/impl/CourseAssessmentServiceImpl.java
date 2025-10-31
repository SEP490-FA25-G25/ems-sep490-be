package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CourseAssessmentRepository;
import org.fyp.tmssep490be.services.CourseAssessmentService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseAssessmentServiceImpl implements CourseAssessmentService {

    private final CourseAssessmentRepository courseAssessmentRepository;
}
