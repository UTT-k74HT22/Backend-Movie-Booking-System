package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    /**
     * Create a new booking
     *
     * @param request the booking request data
     * @return the created booking response
     */
    @Override
    public BookingResponse create(BookingRequest request) {
        return null;
    }

    /**
     * Update an existing booking
     *
     * @param id      the ID of the booking to update
     * @param request the updated booking request data
     * @return the updated booking response
     */
    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        return null;
    }

    /**
     * Delete a booking by ID
     *
     * @param id the ID of the booking to delete
     */
    @Override
    public void delete(Long id) {

    }

    /**
     * Get a booking by ID
     *
     * @param id the ID of the booking to retrieve
     * @return the booking response
     */
    @Override
    public BookingResponse getById(Long id) {
        return null;
    }

    /**
     * Get all bookings with pagination
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of bookings per page
     * @return a paginated response of bookings
     */
    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        return null;
    }
}
