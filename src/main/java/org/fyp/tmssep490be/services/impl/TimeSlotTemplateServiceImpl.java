package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.TimeSlotTemplateRepository;
import org.fyp.tmssep490be.services.TimeSlotTemplateService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimeSlotTemplateServiceImpl implements TimeSlotTemplateService {

    private final TimeSlotTemplateRepository timeSlotTemplateRepository;
}
