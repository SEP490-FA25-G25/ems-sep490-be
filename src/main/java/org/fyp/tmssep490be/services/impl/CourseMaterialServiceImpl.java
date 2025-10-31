package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CourseMaterialRepository;
import org.fyp.tmssep490be.services.CourseMaterialService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CourseMaterialServiceImpl implements CourseMaterialService {

    private final CourseMaterialRepository courseMaterialRepository;
}
