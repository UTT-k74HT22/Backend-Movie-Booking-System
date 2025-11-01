package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SeatController {

    private final SeatService seatService;

    /**
     * Create a new seat
     *
     * @param request seat request object
     * @return seat response object
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid SeatRequest request) {
        log.info("[SEAT-CONTROLLER] Create seat request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(seatService.create(request)));
    }

    /**
     * Update an existing seat
     *
     * @param seatId seat id
     * @param request   seat request object
     * @return seat response object
     */
    @PatchMapping("/{seatId}")
    public ResponseEntity<?> update(@PathVariable Long seatId, @RequestBody @Valid SeatRequest request) {
        log.info("[SEAT-CONTROLLER] Update seat request: {}, {}", seatId, request);
        return ResponseEntity.ok(BaseResponse.success(seatService.update(seatId, request)));
    }

    /**
     * Delete a seat
     *
     * @param seatId seat id
     */
    @DeleteMapping("/{seatId}")
    public ResponseEntity<?> delete(@PathVariable Long seatId) {
        log.info("[SEAT-CONTROLLER] Delete seat request: {}", seatId);
        seatService.delete(seatId);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get seat by id
     *
     * @param seatId seat id
     * @return seat response object
     */
    @GetMapping("/{seatId}")
    public ResponseEntity<?> getById(@PathVariable Long seatId) {
        log.info("[SEAT-CONTROLLER] Get seat by id request: {}", seatId);
        return ResponseEntity.ok(BaseResponse.success(seatService.getById(seatId)));
    }

    /**
     * Get all seats with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated list of seat response objects
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam int pageNumber, @RequestParam int pageSize) {
        log.info("[SEAT-CONTROLLER] Get all seats request: {}, {}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(seatService.getAlls(pageNumber, pageSize)));
    }
}
