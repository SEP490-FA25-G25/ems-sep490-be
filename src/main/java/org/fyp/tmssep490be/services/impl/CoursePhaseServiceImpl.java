package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CoursePhaseRepository;
import org.fyp.tmssep490be.services.CoursePhaseService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CoursePhaseServiceImpl implements CoursePhaseService {

    private final CoursePhaseRepository coursePhaseRepository;
}
