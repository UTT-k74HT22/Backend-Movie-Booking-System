package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Screen.ScreenRequest;
import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.service.ScreenService;
import com.trainning.movie_booking_system.untils.enums.ScreenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;

    /**
     * Create a new screen
     *
     * @param request screen request object
     * @return screen response object
     */
    @Override
    public ScreenResponse create(ScreenRequest request) {
        return null;
    }

    /**
     * Update an existing screen
     *
     * @param screenId screen id
     * @param request  screen request object
     * @return screen response object
     */
    @Override
    public ScreenResponse update(Long screenId, ScreenRequest request) {
        return null;
    }

    /**
     * Delete a screen
     *
     * @param screenId screen id
     */
    @Override
    public void delete(Long screenId) {

    }

    /**
     * Get a screen by id
     *
     * @param screenId screen id
     * @param status
     * @return screen response object
     */
    @Override
    public ScreenResponse getScreenById(Long screenId, ScreenStatus status) {
        return null;
    }

    /**
     * Get all screens with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated screen response
     */
    @Override
    public PageResponse<?> getAll(int pageNumber, int pageSize) {
        return null;
    }

    /**
     * Count all screens
     *
     * @return total number of screens
     */
    @Override
    public long countAllScreens() {
        return 0;
    }
}
