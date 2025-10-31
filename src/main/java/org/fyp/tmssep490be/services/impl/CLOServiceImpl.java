package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.CLORepository;
import org.fyp.tmssep490be.services.CLOService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CLOServiceImpl implements CLOService {

    private final CLORepository cloRepository;
}
