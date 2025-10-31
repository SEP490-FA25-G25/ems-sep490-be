package org.fyp.tmssep490be.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.center.CenterRequest;
import org.fyp.tmssep490be.dtos.center.CenterResponse;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.services.CenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/centers")
@RequiredArgsConstructor
@Slf4j
public class CenterController {

    private final CenterService centerService;

    @PostMapping
    public ResponseEntity<ResponseObject<CenterResponse>> createCenter(@Valid @RequestBody CenterRequest request) {
        log.info("REST request to create Center: {}", request.getCode());
        CenterResponse response = centerService.createCenter(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseObject.success("Center created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseObject<CenterResponse>> getCenterById(@PathVariable Long id) {
        log.info("REST request to get Center: {}", id);
        CenterResponse response = centerService.getCenterById(id);
        return ResponseEntity.ok(ResponseObject.success(response));
    }

    @GetMapping
    public ResponseEntity<ResponseObject<Page<CenterResponse>>> getAllCenters(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("REST request to get all Centers with pagination: {}", pageable);
        Page<CenterResponse> response = centerService.getAllCenters(pageable);
        return ResponseEntity.ok(ResponseObject.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseObject<CenterResponse>> updateCenter(
            @PathVariable Long id,
            @Valid @RequestBody CenterRequest request) {
        log.info("REST request to update Center: {}", id);
        CenterResponse response = centerService.updateCenter(id, request);
        return ResponseEntity.ok(ResponseObject.success("Center updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseObject<Void>> deleteCenter(@PathVariable Long id) {
        log.info("REST request to delete Center: {}", id);
        centerService.deleteCenter(id);
        return ResponseEntity.ok(ResponseObject.success("Center deleted successfully", null));
    }
}
