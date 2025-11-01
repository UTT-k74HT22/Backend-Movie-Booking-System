package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.service.SeatService;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;


    /**
     * Create a new seat based on the provided SeatRequest.
     *
     * @param request the SeatRequest containing seat details
     * @return the created SeatResponse
     */
    @Override
    public SeatResponse create(SeatRequest request) {
        return null;
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
        return null;
    }

    /**
     * Delete the seat identified by the given ID.
     *
     * @param id     the ID of the seat to be deleted
     */
    @Override
    public void delete(Long id) {

    }

    /**
     * Retrieve a paginated list of all seats.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of seats per page
     * @return a PageResponse containing the list of SeatResponse
     */
    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        return null;
    }

    /**
     * Retrieve a seat by its ID.
     *
     * @param id the ID of the seat to retrieve
     * @return the SeatResponse corresponding to the given ID
     */
    @Override
    public SeatResponse getById(Long id) {
        return null;
    }
}
