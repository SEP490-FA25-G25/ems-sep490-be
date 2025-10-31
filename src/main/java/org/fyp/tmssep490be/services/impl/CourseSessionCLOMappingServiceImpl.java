package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CourseSessionCLOMappingRepository;
import org.fyp.tmssep490be.services.CourseSessionCLOMappingService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseSessionCLOMappingServiceImpl implements CourseSessionCLOMappingService {

    private final CourseSessionCLOMappingRepository courseSessionCloMappingRepository;
}
