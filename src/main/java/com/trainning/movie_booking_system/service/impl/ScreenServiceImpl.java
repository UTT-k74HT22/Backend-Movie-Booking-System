package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Screen.ScreenRequest;
import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Theater;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.TheaterRepository;
import com.trainning.movie_booking_system.service.ScreenService;
import com.trainning.movie_booking_system.untils.enums.ScreenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.trainning.movie_booking_system.mapper.ScreenMapper.toScreenResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private final ScreenRepository screenRepository;
    private final TheaterRepository theaterRepository;

    /**
     * Create a new screen
     *
     * @param request screen request object
     * @return screen response object
     */
    @Override
    public ScreenResponse create(ScreenRequest request) {
        log.info("[SCREEN-SERVICE] Create screen request: {}", request);

        if (screenRepository.existsScreenByName(request.getName())) {
            log.error("[SCREEN-SERVICE] Screen with name {} already exists", request.getName());
            return null;
        }

        Theater theater = theaterRepository.findById(request.getTheaterId())
                .orElseThrow(() -> {;
                    log.error("[SCREEN-SERVICE] Theater with id {} not found", request.getTheaterId());
                    return new RuntimeException("Theater not found");
                });

        Screen screen = buildScreen(request, theater);

        screenRepository.save(screen);

        return toScreenResponse(screen);
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

    //========== PRIVATE METHOD ==========//
    private static Screen buildScreen(ScreenRequest request, Theater theater) {
        return Screen.builder()
                .name(request.getName())
                .totalSeats(request.getTotalSeats())
                .status(ScreenStatus.INACTIVE)
                .theater(theater)
                .build();
    }
}
