package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.mapper.SeatMapper;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.service.SeatService;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;


    /**
     * Create a new seat based on the provided SeatRequest.
     *
     * @param request the SeatRequest containing seat details
     * @return the created SeatResponse
     */
    @Override
    public SeatResponse create(SeatRequest request) {
        log.info("[SEAT-SERVICE] Create seat: {}", request);

        // Validate screen exists
        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> new NotFoundException("Screen not found with id: " + request.getScreenId()));

        // Check if seat already exists in this screen
        if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumber(
                request.getScreenId(), request.getRowLabel(), request.getSeatNumber())) {
            throw new BadRequestException(
                    String.format("Seat %s%d already exists in screen %s",
                            request.getRowLabel(), request.getSeatNumber(), screen.getName()));
        }

        Seat seat = Seat.builder()
                .seatNumber(request.getSeatNumber())
                .rowLabel(request.getRowLabel())
                .seatType(request.getSeatType())
                .status(request.getStatus() != null ? request.getStatus() : SeatStatus.AVAILABLE)
                .screen(screen)
                .build();

        Seat saved = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Created seat with id: {}", saved.getId());
        return SeatMapper.toResponse(saved);
    }

    /**
     * Update an existing seat identified by the given ID with the provided SeatRequest.
     *
     * @param id      the ID of the seat to be updated
     * @param request the SeatRequest containing updated seat details
     * @return the updated SeatResponse
     */
    @Override
    public SeatResponse update(Long id, SeatRequest request) {
        log.info("[SEAT-SERVICE] Update seat id {} with: {}", id, request);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seat not found with id: " + id));

        // Validate screen exists if changing screen
        if (!seat.getScreen().getId().equals(request.getScreenId())) {
            Screen newScreen = screenRepository.findById(request.getScreenId())
                    .orElseThrow(() -> new NotFoundException("Screen not found with id: " + request.getScreenId()));
            seat.setScreen(newScreen);
        }

        // Check for duplicate seat in the same screen (excluding current seat)
        if (!seat.getRowLabel().equals(request.getRowLabel()) || seat.getSeatNumber() != request.getSeatNumber()) {
            if (seatRepository.existsByScreenIdAndRowLabelAndSeatNumberAndIdNot(
                    request.getScreenId(), request.getRowLabel(), request.getSeatNumber(), id)) {
                throw new BadRequestException(
                        String.format("Seat %s%d already exists in this screen",
                                request.getRowLabel(), request.getSeatNumber()));
            }
        }

        seat.setSeatNumber(request.getSeatNumber());
        seat.setRowLabel(request.getRowLabel());
        seat.setSeatType(request.getSeatType());
        if (request.getStatus() != null) {
            seat.setStatus(request.getStatus());
        }

        Seat updated = seatRepository.save(seat);
        log.info("[SEAT-SERVICE] Updated seat with id: {}", updated.getId());
        return SeatMapper.toResponse(updated);
    }

    /**
     * Delete the seat identified by the given ID.
     *
     * @param id     the ID of the seat to be deleted
     */
    @Override
    public void delete(Long id) {
        log.info("[SEAT-SERVICE] Delete seat id {}", id);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seat not found with id: " + id));

        seatRepository.delete(seat);
        log.info("[SEAT-SERVICE] Deleted seat with id: {}", id);
    }

    /**
     * Retrieve a paginated list of all seats.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of seats per page
     * @return a PageResponse containing the list of SeatResponse
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<SeatResponse> getAlls(int pageNumber, int pageSize) {
        log.info("[SEAT-SERVICE] Get all seats, page: {}, size: {}", pageNumber, pageSize);

        Page<Seat> page = seatRepository.findAll(PageRequest.of(pageNumber - 1, pageSize));

        List<SeatResponse> content = page.getContent().stream()
                .map(SeatMapper::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<SeatResponse>builder()
                .content(content)
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Retrieve a seat by its ID.
     *
     * @param id the ID of the seat to retrieve
     * @return the SeatResponse corresponding to the given ID
     */
    @Override
    @Transactional(readOnly = true)
    public SeatResponse getById(Long id) {
        log.info("[SEAT-SERVICE] Get seat by id: {}", id);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Seat not found with id: " + id));

        return SeatMapper.toResponse(seat);
    }

    /**
     * Retrieve all seats by screen ID.
     *
     * @param screenId the ID of the screen
     * @return list of SeatResponse
     */
    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreenId(Long screenId) {
        log.info("[SEAT-SERVICE] Get seats by screen id: {}", screenId);

        // Validate screen exists
        screenRepository.findById(screenId)
                .orElseThrow(() -> new NotFoundException("Screen not found with id: " + screenId));

        List<Seat> seats = seatRepository.findByScreenId(screenId);
        return seats.stream()
                .map(SeatMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve seats by screen ID and status.
     *
     * @param screenId the ID of the screen
     * @param status the status of the seat
     * @return list of SeatResponse
     */
    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreenIdAndStatus(Long screenId, SeatStatus status) {
        log.info("[SEAT-SERVICE] Get seats by screen id: {} and status: {}", screenId, status);

        // Validate screen exists
        screenRepository.findById(screenId)
                .orElseThrow(() -> new NotFoundException("Screen not found with id: " + screenId));

        List<Seat> seats = seatRepository.findByScreenIdAndStatus(screenId, status);
        return seats.stream()
                .map(SeatMapper::toResponse)
                .collect(Collectors.toList());
    }
}
