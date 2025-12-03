package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Movie.MovieResponse;
import com.trainning.movie_booking_system.entity.Movie;

public class MovieMapper {

    /**
     * Map Movie entity to MovieResponse DTO
     *
     * @param movie the Movie entity
     * @return the MovieResponse DTO
     */
    public static MovieResponse toMovieResponse(Movie movie) {
        if (movie == null) return null;

        return MovieResponse.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .genre(movie.getGenre())
                .language(movie.getLanguage())
                .duration(movie.getDuration())
                .releaseDate(movie.getReleaseDate() != null ? movie.getReleaseDate().toString() : null)
                .screeningStartDate(movie.getScreeningStartDate() != null ? movie.getScreeningStartDate().toString() : null)
                .screeningEndDate(movie.getScreeningEndDate() != null ? movie.getScreeningEndDate().toString() : null)
                .allowedStartTime(movie.getAllowedStartTime() != null ? movie.getAllowedStartTime().toString() : null)
                .allowedEndTime(movie.getAllowedEndTime() != null ? movie.getAllowedEndTime().toString() : null)
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .rating(movie.getRating() != null ? movie.getRating().doubleValue() : null)
                .status(movie.getStatus())
                .build();
    }

}

