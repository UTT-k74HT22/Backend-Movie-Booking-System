package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Screen.ScreenRequest;
import com.trainning.movie_booking_system.dto.request.Screen.UpdateScreenRequest;
import com.trainning.movie_booking_system.dto.response.Screen.ScreenResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Theater;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.mapper.ScreenMapper;
import com.trainning.movie_booking_system.mapper.TheaterMapper;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.TheaterRepository;
import com.trainning.movie_booking_system.service.ScreenService;
import com.trainning.movie_booking_system.untils.enums.ScreenStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

        Theater theater = getTheater(request.getTheaterId());
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
    public ScreenResponse update(Long screenId, UpdateScreenRequest request) {
        log.info("[SCREEN-SERVICE] Update screen request: {}", request);

        Screen screen = getScreenById(screenId);
        validAndUpdate(request, screen);
        Screen updated = screenRepository.save(screen);
        return toScreenResponse(updated);
    }

    /**
     * Delete a screen
     *
     * @param screenId screen id
     * @param status screen status
     */
    @Override
    public void delete(Long screenId, ScreenStatus status) {
        log.info("[SCREEN-SERVICE] Delete screen request: {}", status);
        Screen screen = getScreenById(screenId);
        screen.setStatus(status);
        screenRepository.save(screen);
    }

    /**
     * Get a screen by id
     *
     * @param screenId screen id
     * @return screen response object
     */
    @Override
    public ScreenResponse getById(Long screenId) {
        log.info("[SCREEN-SERVICE] Get screen by id request: {}", screenId);
        return toScreenResponse(getScreenById(screenId));
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
        log.info("[SCREEN-SERVICE] Get all screens request: pageNumber={}, pageSize={}", pageNumber, pageSize);

        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Invalid PageNumber and PageSize");
        }

        Page<ScreenResponse> screenResponses = screenRepository.findAll(PageRequest.of(pageNumber, pageSize))
                .map(ScreenMapper::toScreenResponse);
        return PageResponse.of(screenResponses);
    }

    /**
     * Count all screens
     *
     * @return total number of screens
     */
    @Override
    public long countAllScreens() {
        log.info("[SCREEN-SERVICE] Count all screens request");
        return screenRepository.count();
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

    private Screen getScreenById(Long screenId) {
        return screenRepository.findById(screenId)
                .orElseThrow(() -> {
                    log.error("[SCREEN-SERVICE] Screen with id {} not found", screenId);
                    return new BadRequestException("Screen not found");
                });
    }

    private Theater getTheater(Long theaterId) {
        return theaterRepository.findById(theaterId)
                .orElseThrow(() -> {
                    log.error("[SCREEN-SERVICE] Theater with id {} not found", theaterId);
                    return new BadRequestException("Theater not found");
                });
    }

    private void validAndUpdate(UpdateScreenRequest request, Screen screen) {
        if (request.getTheaterId() != null) {
            Theater theater = getTheater(request.getTheaterId());
            screen.setTheater(theater);
        }

        if (request.getName() != null && !request.getName().equals(screen.getName())) {
            boolean exists = screenRepository.existsByNameAndTheaterId(request.getName(), screen.getTheater().getId());
            if (exists) {
                log.error("[SCREEN-SERVICE] UPDATE Screen with name {} already exists", request.getName());
                throw new BadRequestException("Screen with the same name already exists in this theater");
            }
            screen.setName(request.getName());
        }

        if (request.getTotalSeats() != null) {
            screen.setTotalSeats(request.getTotalSeats());
        }

        if (request.getStatus() != null) {
            screen.setStatus(request.getStatus());
        }
    }
}
