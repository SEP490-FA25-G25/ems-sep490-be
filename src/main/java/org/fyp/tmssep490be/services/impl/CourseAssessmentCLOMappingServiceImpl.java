package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CourseAssessmentCLOMappingRepository;
import org.fyp.tmssep490be.services.CourseAssessmentCLOMappingService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseAssessmentCLOMappingServiceImpl implements CourseAssessmentCLOMappingService {

    private final CourseAssessmentCLOMappingRepository courseAssessmentCloMappingRepository;
}
