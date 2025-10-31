package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.StudentRequestRepository;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentRequestServiceImpl implements StudentRequestService {

    private final StudentRequestRepository studentRequestRepository;
}
