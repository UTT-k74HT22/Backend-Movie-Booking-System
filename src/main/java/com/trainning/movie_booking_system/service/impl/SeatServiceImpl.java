package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.SeatMapper;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.service.SeatService;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final SeatMapper seatMapper;

    /**
     * Create a new seat
     *
     * @param request seat request object
     * @return seat response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", key = "#request.screenId"),
            @CacheEvict(value = "seat:detail", allEntries = true)
    })
    public SeatResponse create(SeatRequest request) {
        log.info("[SEAT-SERVICE] Create seat request: {}", request);

        // Check if seat already exists
        if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(
                request.getScreenId(),
                request.getRowLabel(),
                request.getSeatNumber())) {
            throw new BadRequestException(
                    String.format("Seat %s-%s already exists in screen ID %d",
                            request.getRowLabel(),
                            request.getSeatNumber(),
                            request.getScreenId())
            );
        }

        // Get screen
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new NotFoundException(
                        String.format("Screen not found with id: %d", request.getScreenId())
                ));

        // Create seat
        Seat seat = seatMapper.toEntity(request);
        seat.setScreen(screen);

        // Save seat
        Seat savedSeat = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Seat created successfully with id: {}", savedSeat.getId());

        return seatMapper.toResponse(savedSeat);
    }

    /**
     * Update an existing seat
     *
     * @param seatId  seat id
     * @param request seat request object
     * @return seat response object
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", allEntries = true),
            @CacheEvict(value = "seat:detail", key = "#seatId")
    })
    public SeatResponse update(Long seatId, SeatRequest request) {
        log.info("[SEAT-SERVICE] Update seat request: {}, {}", seatId, request);

        // Get seat
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat not found with id: %d", seatId)
                ));

        // Check if updating to a seat number that already exists
        if (request.getRowLabel() != null && request.getSeatNumber() != null) {
            if (!seat.getRowLabel().equals(request.getRowLabel()) ||
                    seat.getSeatNumber() != request.getSeatNumber()) {
                if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(
                        seat.getScreen().getId(),
                        request.getRowLabel(),
                        request.getSeatNumber())) {
                    throw new BadRequestException(
                            String.format("Seat %s-%s already exists in this screen",
                                    request.getRowLabel(),
                                    request.getSeatNumber())
                    );
                }
            }
        }

        // Update screen if provided
        if (request.getScreenId() != null && !seat.getScreen().getId().equals(request.getScreenId())) {
            Screen newScreen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new NotFoundException(
                            String.format("Screen not found with id: %d", request.getScreenId())
                    ));
            seat.setScreen(newScreen);
        }

        // Update seat
        seatMapper.updateEntity(seat, request);

        // Save seat
        Seat updatedSeat = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Seat updated successfully with id: {}", updatedSeat.getId());

        return seatMapper.toResponse(updatedSeat);
    }

    /**
     * Delete a seat
     *
     * @param seatId seat id
     */
    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = "seats:screen", allEntries = true),
            @CacheEvict(value = "seat:detail", key = "#seatId")
    })
    public void delete(Long seatId) {
        log.info("[SEAT-SERVICE] Delete seat request: {}", seatId);

        // Check if seat exists
        if (!seatRepository.existsById(seatId)) {
            throw new NotFoundException(
                    String.format("Seat not found with id: %d", seatId)
            );
        }

        // Delete seat
        seatRepository.deleteById(seatId);
        log.info("[SEAT-SERVICE] Seat deleted successfully with id: {}", seatId);
    }

    /**
     * Get seat by id
     *
     * @param seatId seat id
     * @return seat response object
     */
    @Override
    @Cacheable(value = "seat:detail", key = "#seatId")
    public SeatResponse getById(Long seatId) {
        log.info("[SEAT-SERVICE] Get seat by id request: {}", seatId);

        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Seat not found with id: %d", seatId)
                ));

        return seatMapper.toResponse(seat);
    }

    /**
     * Get all seats with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated list of seat response objects
     */
    @Override
    public Page<SeatResponse> getAlls(int pageNumber, int pageSize) {
        log.info("[SEAT-SERVICE] Get all seats request: {}, {}", pageNumber, pageSize);

        Page<Seat> seats = seatRepository.findAll(PageRequest.of(pageNumber, pageSize));

        return seats.map(seatMapper::toResponse);
    }

    /**
     * Get all seats by screen ID
     *
     * @param screenId screen id
     * @return list of seat response objects
     */
    @Override
    @Cacheable(value = "seats:screen", key = "#screenId")
    public List<SeatResponse> getSeatsByScreenId(Long screenId) {
        log.info("[SEAT-SERVICE] Get seats by screen id request: {}", screenId);

        // Check if screen exists
        if (!screenRepository.existsById(screenId)) {
            throw new NotFoundException(
                    String.format("Screen not found with id: %d", screenId)
            );
        }

        List<Seat> seats = seatRepository.findAllByScreenIdOrderByRowLabelAndSeatNumber(screenId);

        return seats.stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get seats by screen ID and status
     *
     * @param screenId screen id
     * @param status   seat status
     * @return list of seat response objects
     */
    @Override
    @Cacheable(value = "seats:screen:status", key = "#screenId + ':' + #status")
    public List<SeatResponse> getSeatsByScreenIdAndStatus(Long screenId, SeatStatus status) {
        log.info("[SEAT-SERVICE] Get seats by screen id: {} and status: {}", screenId, status);

        // Check if screen exists
        if (!screenRepository.existsById(screenId)) {
            throw new NotFoundException(
                    String.format("Screen not found with id: %d", screenId)
            );
        }

        List<Seat> seats = seatRepository.findByScreenIdAndStatus(screenId, status);

        return seats.stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }
}
