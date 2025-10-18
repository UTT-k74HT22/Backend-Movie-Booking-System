package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Theater.TheaterRequest;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.untils.enums.TheaterStatus;

public interface TheaterService {

    /**
     * Create a new theater
     * @param request theater request object
     * @return theater response object
     */
    TheaterResponse create(TheaterRequest request);

    /**
     * Update an existing theater
     * @param theaterId theater id
     * @param request theater request object
     * @return theater response object
     */
    TheaterResponse update(Long theaterId, TheaterRequest request);

    /**
     * Delete a theater
     *
     * @param theaterId theater id
     * @param status
     */
    void delete(Long theaterId, TheaterStatus status);

    /**
     * Get a theater by id
     * @param theaterId theater id
     * @return theater response object
     */
    TheaterResponse getById(Long theaterId);

    /**
     * Get all theaters with pagination
     * @param pageNumber page number
     * @param pageSize page size
     * @return paginated theater response
     */
    PageResponse<?> getAlls(int pageNumber, int pageSize);

    /**
     * Count total number of theaters
     * @return total count of theaters
     */
    long countTheaters();
}
