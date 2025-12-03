package com.trainning.movie_booking_system.service.impl.Movie;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.request.Movie.UpdateMovieRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.MovieMapper;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.service.Movie.MovieService;
import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static com.trainning.movie_booking_system.mapper.MovieMapper.toMovieResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Transactional
    @Override
    public MovieResponse create(MovieRequest request) {
        log.info("[MOVIE SERVICE] - Create movie request: {}", request);

        if (movieRepository.existsByTitle(request.getTitle().trim())) {
            throw new BadRequestException("Movie with the given title already exists");
        }

        // Parse releaseDate
        LocalDate releaseDate = null;
        if (request.getReleaseDate() != null && !request.getReleaseDate().isBlank()) {
            releaseDate = LocalDate.parse(request.getReleaseDate().trim());
        }

        // Parse screeningStartDate and screeningEndDate
        LocalDate screeningStartDate = null;
        LocalDate screeningEndDate = null;
        if (request.getScreeningStartDate() != null && !request.getScreeningStartDate().isBlank()) {
            screeningStartDate = LocalDate.parse(request.getScreeningStartDate().trim());
        }
        if (request.getScreeningEndDate() != null && !request.getScreeningEndDate().isBlank()) {
            screeningEndDate = LocalDate.parse(request.getScreeningEndDate().trim());
        }

        // Parse allowedStartTime and allowedEndTime
        LocalTime allowedStartTime = null;
        LocalTime allowedEndTime = null;
        if (request.getAllowedStartTime() != null && !request.getAllowedStartTime().isBlank()) {
            allowedStartTime = LocalTime.parse(request.getAllowedStartTime().trim());
        }
        if (request.getAllowedEndTime() != null && !request.getAllowedEndTime().isBlank()) {
            allowedEndTime = LocalTime.parse(request.getAllowedEndTime().trim());
        }

        Movie movie = buildMovie(request, releaseDate, screeningStartDate, screeningEndDate, allowedStartTime, allowedEndTime);
        movieRepository.save(movie);

        return toMovieResponse(movie);
    }


    @Transactional
    @Override
    public MovieResponse update(Long movieId, UpdateMovieRequest request) {
        log.info("[MOVIE SERVICE] - Update movie ID: {}, request: {}", movieId, request);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            if (!movie.getTitle().equalsIgnoreCase(request.getTitle().trim())
                    && movieRepository.existsByTitle(request.getTitle().trim())) {
                throw new BadRequestException("Movie with the given title already exists");
            }
            movie.setTitle(request.getTitle().trim());
        }

        updateFields(request, movie);
        movieRepository.save(movie);

        return toMovieResponse(movie);
    }

    @Transactional
    @Override
    public void delete(Long movieId, MovieStatus movieStatus) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));
        movie.setStatus(movieStatus != null ? movieStatus : MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    @Override
    public MovieResponse getById(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found with ID: " + movieId));
        return toMovieResponse(movie);
    }

    @Override
    public PageResponse<?> getAll(int pageNumber, int pageSize) {
        if (pageNumber < 0 || pageSize < 0) {
            throw new BadRequestException("Invalid PageNumber and PageSize");
        }

        Page<MovieResponse> movieResponse = movieRepository.findAll(PageRequest.of(pageNumber, pageSize))
                .map(MovieMapper::toMovieResponse);
        return PageResponse.of(movieResponse);
    }

    @Override
    public long countTotalMovies() {
        return movieRepository.count();
    }

    //====================== PRIVATE METHODS ====================//

    private static Movie buildMovie(MovieRequest request,
                                     LocalDate releaseDate,
                                     LocalDate screeningStartDate,
                                     LocalDate screeningEndDate,
                                     LocalTime allowedStartTime,
                                     LocalTime allowedEndTime) {
        return Movie.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .genre(request.getGenre())
                .language(request.getLanguage())
                .duration(request.getDuration())
                .releaseDate(releaseDate)
                .screeningStartDate(screeningStartDate)
                .screeningEndDate(screeningEndDate)
                .allowedStartTime(allowedStartTime)
                .allowedEndTime(allowedEndTime)
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .rating(request.getRating() != null ? BigDecimal.valueOf(request.getRating()) : null)
                .status(request.getStatus())
                .build();
    }


    private static void updateFields(UpdateMovieRequest request, Movie movie) {
        if (request.getDescription() != null) movie.setDescription(request.getDescription());
        if (request.getDuration() != null) movie.setDuration(request.getDuration());
        if (request.getReleaseDate() != null) movie.setReleaseDate(parseDate(request.getReleaseDate(), "releaseDate"));
        if (request.getPosterUrl() != null) movie.setPosterUrl(request.getPosterUrl());
        if (request.getTrailerUrl() != null) movie.setTrailerUrl(request.getTrailerUrl());
        if (request.getRating() != null) movie.setRating(BigDecimal.valueOf(request.getRating()));
        if (request.getGenre() != null) movie.setGenre(request.getGenre());
        if (request.getLanguage() != null) movie.setLanguage(request.getLanguage());
        if (request.getStatus() != null) movie.setStatus(request.getStatus());
        if (request.getScreeningStartDate() != null) movie.setScreeningStartDate(parseDate(request.getScreeningStartDate(), "screeningStartDate"));
        if (request.getScreeningEndDate() != null) movie.setScreeningEndDate(parseDate(request.getScreeningEndDate(), "screeningEndDate"));
        if (request.getAllowedStartTime() != null) movie.setAllowedStartTime(parseTime(request.getAllowedStartTime(), "allowedStartTime"));
        if (request.getAllowedEndTime() != null) movie.setAllowedEndTime(parseTime(request.getAllowedEndTime(), "allowedEndTime"));
    }

    private static LocalDate parseDate(String dateStr, String fieldName) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid " + fieldName + " format. Expected yyyy-MM-dd");
        }
    }

    private static LocalTime parseTime(String timeStr, String fieldName) {
        if (timeStr == null || timeStr.isBlank()) return null;
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid " + fieldName + " format. Expected HH:mm");
        }
    }
}
