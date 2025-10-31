package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.PLORepository;
import org.fyp.tmssep490be.services.PLOService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PLOServiceImpl implements PLOService {

    private final PLORepository ploRepository;
}
