package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.service.MovieService;
import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import static com.trainning.movie_booking_system.mapper.MovieMapper.toMovieResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    /**
     * Create a new movie
     *
     * @param request the movie request
     * @return the created movie response
     */
    @Transactional
    @Override
    public MovieResponse create(MovieRequest request) {
        log.info("[MOVIE SERVICE] - Create movie request: {}", request);

        if (movieRepository.existsByTitle(request.getTitle().trim())) {
            log.error("[MOVIE SERVICE] - Movie with title '{}' already exists", request.getTitle());
            throw new BadRequestException("Movie with the given title already exists");
        }

        LocalDate releaseDate = null;
        if (request.getReleaseDate() != null && !request.getReleaseDate().isBlank()) {
            try {
                releaseDate = LocalDate.parse(request.getReleaseDate().trim());
            } catch (DateTimeParseException e) {
                log.error("[MOVIE SERVICE] - Invalid release date: {}", request.getReleaseDate());
                throw new BadRequestException("Invalid release date format. Expected yyyy-MM-dd");
            }
        }

        Movie movie = buildMovie(request, releaseDate);
        movieRepository.save(movie);
        log.info("[MOVIE SERVICE] - Movie created successfully with ID: {}", movie.getId());

        return toMovieResponse(movie);
    }

    /**
     * Update an existing movie
     *
     * @param movieId the ID of the movie to update
     * @param request the movie request
     * @return the updated movie response
     */
    @Transactional
    @Override
    public MovieResponse update(Long movieId, MovieRequest request) {
        return null;
    }

    /**
     * Delete a movie by its ID
     *
     * @param movieId     the ID of the movie to delete
     * @param movieStatus the status of the movie
     */
    @Transactional
    @Override
    public void delete(Long movieId, MovieStatus movieStatus) {

    }

    /**
     * Get a movie by its ID
     *
     * @param movieId the ID of the movie to retrieve
     * @return the movie response
     */
    @Override
    public MovieResponse getById(Long movieId) {
        return null;
    }

    /**
     * Get all movies with pagination
     *
     * @param page the page number
     * @param size the size of the page
     * @return a paginated response of movies
     */
    @Override
    public PageResponse<?> getAll(int page, int size) {
        return null;
    }

    /**
     * Count total number of movies
     *
     * @return the total count of movies
     */
    @Override
    public long countTotalMovies() {
        return 0;
    }

    //====================================== Private methods =====================================//
    private static Movie buildMovie(MovieRequest request, LocalDate releaseDate) {
        return Movie.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .genre(request.getGenre())
                .language(request.getLanguage())
                .duration(request.getDuration())
                .releaseDate(releaseDate)
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .rating(request.getRating() != null ? BigDecimal.valueOf(request.getRating()) : null)
                .status(request.getStatus())
                .build();
    }
}
