package com.trainning.movie_booking_system.dto.response.Movie;

import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovieResponse {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private Double rating;
    private String genre;
    private String language;
    private MovieStatus status;
}
