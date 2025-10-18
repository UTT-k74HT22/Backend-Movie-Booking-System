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
    private String director;
    private String genre;
    private int duration; // in minutes
    private String releaseDate; // ISO format
    private MovieStatus status; // e.g., COMING_SOON, NOW_SHOWING, ENDED
}
