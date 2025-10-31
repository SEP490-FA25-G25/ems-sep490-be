package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.StudentFeedbackResponseRepository;
import org.fyp.tmssep490be.services.StudentFeedbackResponseService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentFeedbackResponseServiceImpl implements StudentFeedbackResponseService {

    private final StudentFeedbackResponseRepository studentFeedbackResponseRepository;
}
