package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.FeedbackQuestionRepository;
import org.fyp.tmssep490be.services.FeedbackQuestionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeedbackQuestionServiceImpl implements FeedbackQuestionService {

    private final FeedbackQuestionRepository feedbackQuestionRepository;
}
