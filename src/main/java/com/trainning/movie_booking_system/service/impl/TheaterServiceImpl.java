package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Theater.TheaterRequest;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.dto.response.Theater.TheaterResponse;
import com.trainning.movie_booking_system.entity.Theater;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.TheaterRepository;
import com.trainning.movie_booking_system.service.TheaterService;
import com.trainning.movie_booking_system.untils.enums.TheaterStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.trainning.movie_booking_system.mapper.TheaterMapper.toTheaterResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class TheaterServiceImpl  implements TheaterService {

    private final TheaterRepository theaterRepository;

    /**
     * Create a new theater
     *
     * @param request theater request object
     * @return theater response object
     */
    @Transactional
    @Override
    public TheaterResponse create(TheaterRequest request) {
        log.info("[THEATER SERVICE] Creating new theater with name: {}", request.getName());

        if (theaterRepository.existsByName(request.getName())) {
            log.error("[THEATER SERVICE] Theater with name '{}' already exists", request.getName());
            throw new BadRequestException("Theater with the same name already exists");
        }

        Theater theater = Theater.builder()
                .name(request.getName())
                .location(request.getLocation())
                .city(request.getCity())
                .phone(request.getPhone())
                .status(TheaterStatus.INACTIVE)
                .build();

        theaterRepository.save(theater);

        log.info("[THEATER SERVICE] Theater '{}' created successfully with ID: {}", theater.getName(), theater.getId());

        return toTheaterResponse(theater);
    }

    /**
     * Update an existing theater
     *
     * @param theaterId theater id
     * @param request   theater request object
     * @return theater response object
     */
    @Transactional
    @Override
    public TheaterResponse update(Long theaterId, TheaterRequest request) {
        return null;
    }

    /**
     * Delete a theater
     *
     * @param theaterId theater id
     * @param status
     */
    @Transactional
    @Override
    public void delete(Long theaterId, TheaterStatus status) {

    }

    /**
     * Get a theater by id
     *
     * @param theaterId theater id
     * @return theater response object
     */
    @Override
    public TheaterResponse getById(Long theaterId) {
        return null;
    }

    /**
     * Get all theaters with pagination
     *
     * @param pageNumber page number
     * @param pageSize   page size
     * @return paginated theater response
     */
    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        return null;
    }

    /**
     * Count total number of theaters
     *
     * @return total count of theaters
     */
    @Override
    public long countTheaters() {
        return 0;
    }

    //=========================================== PRIVATE METHOD ===========================================//
}

