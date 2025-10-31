package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.TeachingSlotRepository;
import org.fyp.tmssep490be.services.TeachingSlotService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TeachingSlotServiceImpl implements TeachingSlotService {

    private final TeachingSlotRepository teachingSlotRepository;
}
