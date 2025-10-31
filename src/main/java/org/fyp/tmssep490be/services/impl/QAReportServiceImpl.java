package org.fyp.tmssep490be.services.impl;

import lombok.RequiredArgsConstructor;
import org.fyp.tmssep490be.repositories.QAReportRepository;
import org.fyp.tmssep490be.services.QAReportService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QAReportServiceImpl implements QAReportService {

    private final QAReportRepository qaReportRepository;
}
