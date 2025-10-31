package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CourseSessionRepository;
import org.fyp.tmssep490be.services.CourseSessionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseSessionServiceImpl implements CourseSessionService {

    private final CourseSessionRepository courseSessionRepository;
}
