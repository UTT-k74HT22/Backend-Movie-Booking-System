package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.ShowtimeService;
import com.trainning.movie_booking_system.untils.enums.ShowtimeStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
@Validated
@Slf4j
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    /**
     * Create a new showtime
     *
     * @param request showtime request object
     * @return showtime response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid ShowtimeRequest request) {
        log.info("[SHOWTIME-CONTROLLER] Create showtime request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.create(request)));
    }

    /**
     * Update an existing showtime
     *
     * @param showtimeId showtime id
     * @param request    showtime request object
     * @return showtime response object
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @PatchMapping("/{showtimeId}")
    public ResponseEntity<?> update(@PathVariable Long showtimeId, @RequestBody @Valid UpdateShowtimeRequest request) {
        log.info("[SHOWTIME-CONTROLLER] Update showtime request: {}, {}", showtimeId, request);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.update(showtimeId, request)));
    }

    /**
     * Delete a showtime (soft delete)
     *
     * @param showtimeId showtime id
     * @param status     showtime status
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<?> delete(@PathVariable Long showtimeId, @RequestParam ShowtimeStatus status) {
        log.info("[SHOWTIME-CONTROLLER] Delete showtime request: {}, with status: {}", showtimeId, status);
        showtimeService.delete(showtimeId, status);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a showtime by id
     *
     * @param showtimeId showtime id
     * @return showtime response object
     */
    @GetMapping("/{showtimeId}")
    public ResponseEntity<?> getById(@PathVariable Long showtimeId) {
        log.info("[SHOWTIME-CONTROLLER] Get showtime by id request: {}", showtimeId);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.getById(showtimeId)));
    }

    /**
     * Get all showtimes with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated showtime response
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam(defaultValue = "0") int pageNumber,
                                     @RequestParam(defaultValue = "10") int pageSize) {
        log.info("[SHOWTIME-CONTROLLER] Get all showtimes request: pageNumber={}, pageSize={}", pageNumber, pageSize);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.getAll(pageNumber, pageSize)));
    }

    /**
     * Get showtimes by theater and movie on a specific date
     *
     * @param theaterId theater id
     * @param movieId   movie id
     * @param date      date to filter showtimes
     * @return list of showtimes grouped by screen
     */
    @GetMapping("/by-theater-and-movie")
    public ResponseEntity<?> getShowtimes(
            @RequestParam Long theaterId,
            @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("[SHOWTIME CONTROLLER] theaterId={}, movieId={}, date={}", theaterId, movieId, date);
        return ResponseEntity.ok(BaseResponse.success(showtimeService.findByTheaterAndMovie(theaterId, movieId, date)));
    }


    /**
     * Count total number of showtimes
     *
     * @return total count of showtimes
     */
    @GetMapping("/count")
    public ResponseEntity<?> countShowtimes() {
        log.info("[SHOWTIME-CONTROLLER] Count showtimes request");
        return ResponseEntity.ok(BaseResponse.success(showtimeService.countShowtime()));
    }
}
