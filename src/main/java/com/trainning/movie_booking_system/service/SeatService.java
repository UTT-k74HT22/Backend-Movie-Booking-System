package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Seat.SeatRequest;
import com.trainning.movie_booking_system.dto.response.Seat.SeatResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.untils.enums.SeatStatus;

import java.util.List;

public interface SeatService {

    /**
     * Create a new seat based on the provided SeatRequest.
     *
     * @param request the SeatRequest containing seat details
     * @return the created SeatResponse
     */
    SeatResponse create(SeatRequest request);

    /**
     * Update an existing seat identified by the given ID with the provided SeatRequest.
     *
     * @param id the ID of the seat to be updated
     * @param request the SeatRequest containing updated seat details
     * @return the updated SeatResponse
     */
    SeatResponse update(Long id, SeatRequest request);

    /**
     * Delete the seat identified by the given ID.
     *
     * @param id the ID of the seat to be deleted
     */
    void delete(Long id);

    /**
     * Retrieve a paginated list of all seats.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize the number of seats per page
     * @return a PageResponse containing the list of SeatResponse
     */
    PageResponse<SeatResponse> getAlls(int pageNumber, int pageSize);

    /**
     * Retrieve a seat by its ID.
     *
     * @param id the ID of the seat to retrieve
     * @return the SeatResponse corresponding to the given ID
     */
    SeatResponse getById(Long id);

    /**
     * Retrieve all seats by screen ID.
     *
     * @param screenId the ID of the screen
     * @return list of SeatResponse
     */
    List<SeatResponse> getSeatsByScreenId(Long screenId);

    /**
     * Retrieve seats by screen ID and status.
     *
     * @param screenId the ID of the screen
     * @param status the status of the seat
     * @return list of SeatResponse
     */
    List<SeatResponse> getSeatsByScreenIdAndStatus(Long screenId, SeatStatus status);
}
