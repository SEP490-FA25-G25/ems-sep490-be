package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.BranchRepository;
import org.fyp.tmssep490be.services.BranchService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
}
