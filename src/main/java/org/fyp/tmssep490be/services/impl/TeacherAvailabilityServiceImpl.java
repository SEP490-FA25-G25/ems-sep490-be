package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.TeacherAvailabilityRepository;
import org.fyp.tmssep490be.services.TeacherAvailabilityService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherAvailabilityServiceImpl implements TeacherAvailabilityService {

    private final TeacherAvailabilityRepository teacherAvailabilityRepository;
}
