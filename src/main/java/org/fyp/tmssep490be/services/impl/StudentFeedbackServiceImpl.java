package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.StudentFeedbackRepository;
import org.fyp.tmssep490be.services.StudentFeedbackService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentFeedbackServiceImpl implements StudentFeedbackService {

    private final StudentFeedbackRepository studentFeedbackRepository;
}
