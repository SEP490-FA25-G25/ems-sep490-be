package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.PLOCLOMappingRepository;
import org.fyp.tmssep490be.services.PLOCLOMappingService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PLOCLOMappingServiceImpl implements PLOCLOMappingService {

    private final PLOCLOMappingRepository ploCloMappingRepository;
}
