package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.service.ShowtimeService;
import com.trainning.movie_booking_system.untils.enums.ShowtimeStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;

    /**
     * Create a new showtime.
     *
     * @param request the showtime request data
     * @return the created showtime response
     */
    @Override
    public ShowtimeResponse create(ShowtimeRequest request) {
        return null;
    }

    /**
     * Update an existing showtime.
     *
     * @param id      the ID of the showtime to update
     * @param request the updated showtime request data
     * @return the updated showtime response
     */
    @Override
    public ShowtimeResponse update(Long id, UpdateShowtimeRequest request) {
        return null;
    }

    /**
     * Delete a showtime by its ID.
     *
     * @param id the ID of the showtime to delete
     */
    @Override
    public void delete(Long id, ShowtimeStatus status) {

    }

    /**
     * Get a showtime by its ID.
     *
     * @param id the ID of the showtime to retrieve
     * @return the showtime response
     */
    @Override
    public ShowtimeResponse getById(Long id) {
        return null;
    }

    /**
     * Get all showtimes with pagination.
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of showtimes per page
     * @return a paginated response of showtimes
     */
    @Override
    public PageResponse<?> getAll(int pageNumber, int pageSize) {
        return null;
    }

    /**
     * Count the total number of showtimes.
     *
     * @return the total count of showtimes
     */
    @Override
    public long countShowtime() {
        return 0;
    }
}
