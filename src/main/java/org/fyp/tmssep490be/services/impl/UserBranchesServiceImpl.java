package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.UserBranchesRepository;
import org.fyp.tmssep490be.services.UserBranchesService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserBranchesServiceImpl implements UserBranchesService {

    private final UserBranchesRepository userBranchesRepository;
}
