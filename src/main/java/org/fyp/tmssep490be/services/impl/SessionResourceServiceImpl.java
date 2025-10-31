package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.SessionResourceRepository;
import org.fyp.tmssep490be.services.SessionResourceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionResourceServiceImpl implements SessionResourceService {

    private final SessionResourceRepository sessionResourceRepository;
}
