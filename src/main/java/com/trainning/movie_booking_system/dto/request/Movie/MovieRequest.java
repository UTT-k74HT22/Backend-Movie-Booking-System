package com.trainning.movie_booking_system.dto.request.Movie;

import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class MovieRequest {
    @NotBlank(message = "Title is mandatory")
    private String title;

    private String description;

    @NotBlank(message = "Director is mandatory")
    private String director;

    @NotBlank(message = "Genre is mandatory")
    private String genre;

    @NotBlank(message = "Duration is mandatory")
    private int duration; // in minutes

    @NotBlank(message = "Release date is mandatory")
    private String releaseDate; // ISO format

    @NotBlank(message = "Status is mandatory")
    private MovieStatus status;
}
