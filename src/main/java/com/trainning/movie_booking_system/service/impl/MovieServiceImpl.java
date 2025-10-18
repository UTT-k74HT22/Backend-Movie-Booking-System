package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Movie.MovieRequest;
import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.repository.MovieRepository;
import com.trainning.movie_booking_system.service.MovieService;
import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        return null;
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
}
