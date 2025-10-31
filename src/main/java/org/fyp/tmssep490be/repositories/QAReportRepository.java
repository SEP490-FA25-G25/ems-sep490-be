package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.QAReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QAReportRepository extends JpaRepository<QAReport, Long> {
}
