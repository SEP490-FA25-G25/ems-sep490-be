package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.center.CenterRequest;
import org.fyp.tmssep490be.dtos.center.CenterResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CenterService {

    CenterResponse createCenter(CenterRequest request);

    CenterResponse getCenterById(Long id);

    Page<CenterResponse> getAllCenters(Pageable pageable);

    CenterResponse updateCenter(Long id, CenterRequest request);

    void deleteCenter(Long id);
}
