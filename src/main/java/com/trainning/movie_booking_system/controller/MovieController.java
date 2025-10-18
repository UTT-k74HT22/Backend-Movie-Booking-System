package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.MovieService;
import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MovieController {

    private final MovieService movieService;

    /**
     * Create a new movie
     *
     * @param request the movie request
     * @return the created movie response
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody @Valid MovieRequest request) {
        log.info("[MOVIE-CONTROLLER] Create movie request: {}", request);
        return ResponseEntity.ok(BaseResponse.success(movieService.create(request)));
    }

    /**
     * Update an existing movie
     * @param movieId the movie ID
     * @param request the movie request
     * @return the updated movie response
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @PatchMapping("/{movieId}")
    public ResponseEntity<?> update(@PathVariable Long movieId, @RequestBody @Valid MovieRequest request) {
        log.info("[MOVIE-CONTROLLER] Update movie request: {}, {}", movieId, request);
        return ResponseEntity.ok(BaseResponse.success(movieService.update(movieId, request)));
    }

    /**
     * Delete a movie by its ID
     *
     * @param movieId     the ID of the movie to delete
     * @param movieStatus the status of the movie
     * @return ResponseEntity indicating the result of the delete operation
     */
    @PreAuthorize(value = "hasAnyAuthority('ROLE_ADMIN', 'ROLE_THEATER_MANAGEMENT')")
    @DeleteMapping("/{movieId}")
    public ResponseEntity<?> delete(@PathVariable Long movieId, @RequestParam MovieStatus movieStatus) {
        log.info("[MOVIE-CONTROLLER] Delete movie request: {}, {}", movieId, movieStatus);
        movieService.delete(movieId, movieStatus);
        return ResponseEntity.ok(BaseResponse.success());
    }

    /**
     * Get a movie by its ID
     * @param movieId the ID of the movie to retrieve
     * @return the movie response
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getById(@PathVariable Long movieId) {
        log.info("[MOVIE-CONTROLLER] Get movie by ID request: {}", movieId);
        return ResponseEntity.ok(BaseResponse.success(movieService.getById(movieId)));
    }

    /**
     * Get all movies with pagination
     * @param pageNumber the page number
     * @param pageSize the size of the page
     * @return a paginated response of movies
     */
    @GetMapping
    public ResponseEntity<?> getAlls(@RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
                                     @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        log.info("[MOVIE-CONTROLLER] Get all movies request: {}, {}", pageSize, pageNumber);
        return ResponseEntity.ok(BaseResponse.success(movieService.getAll(pageNumber, pageNumber)));
    }

    /**
     * Count total number of movies
     * @return the total count of movies
     */
    @GetMapping("/count")
    public ResponseEntity<?> countTotalMovies() {
        log.info("[MOVIE-CONTROLLER] Count total movies request");
        return ResponseEntity.ok(BaseResponse.success(movieService.countTotalMovies()));
    }
}
