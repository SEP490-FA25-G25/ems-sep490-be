package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.StudentSessionRepository;
import org.fyp.tmssep490be.services.StudentSessionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentSessionServiceImpl implements StudentSessionService {

    private final StudentSessionRepository studentSessionRepository;
}
