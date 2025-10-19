package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Showtime.ShowtimeRequest;
import com.trainning.movie_booking_system.dto.request.Showtime.UpdateShowtimeRequest;
import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Screen;
import com.trainning.movie_booking_system.entity.Showtime;
import com.trainning.movie_booking_system.entity.Theater;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.ScreenRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.service.ShowtimeService;
import com.trainning.movie_booking_system.untils.enums.ShowtimeStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.trainning.movie_booking_system.mapper.ShowtimeMapper.toShowtimeResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ScreenRepository screenRepository;

    /**
     * Create a new showtime.
     *
     * @param request the showtime request data
     * @return the created showtime response
     */
    @Transactional
    @Override
    public ShowtimeResponse create(ShowtimeRequest request) {
        log.info("[SHOWTIME SERVICE]: Creating new showtime with request: {}", request);

        Screen screen = screenRepository.findById(request.getScreenId())
                .orElseThrow(() -> {
                    log.error("[SHOWTIME SERVICE]: Screen with ID {} not found", request.getScreenId());
                    return new BadRequestException("Screen not found");
                });

        Theater theater = screen.getTheater();

        if (showtimeRepository.existsByScreenIdAndShowDateAndStartTime(request.getScreenId(),
                request.getShowDate(), request.getStartTime())) {

            log.error("[SHOWTIME SERVICE]: Showtime already exists for screen ID {}, date {}, and start time {}",
                    request.getScreenId(), request.getShowDate(), request.getStartTime());
            throw new BadRequestException("Showtime already exists for the given screen, date, and time");
        }

        Showtime showtime = Showtime.builder()
                .movieId(request.getMovieId()) // mock tạm, sau này đổi sang Movie entity
                .screen(screen)
                .showDate(request.getShowDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .price(request.getPrice())
                .status(ShowtimeStatus.ACTIVE)
                .build();
        showtimeRepository.save(showtime);

        return toShowtimeResponse(showtime);
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
