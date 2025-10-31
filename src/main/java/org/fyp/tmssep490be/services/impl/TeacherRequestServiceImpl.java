package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.TeacherRequestRepository;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeacherRequestServiceImpl implements TeacherRequestService {

    private final TeacherRequestRepository teacherRequestRepository;
}
