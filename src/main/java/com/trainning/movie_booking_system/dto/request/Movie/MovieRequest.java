package com.trainning.movie_booking_system.dto.request.Movie;

import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Title is mandatory")
    private String title;

    private String description;

    @NotNull(message = "Duration is mandatory")
    @Positive(message = "Duration must be greater than 0")
    private Integer duration;

    private String releaseDate; // ISO format (yyyy-MM-dd)

    private String posterUrl;
    private String trailerUrl;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be >= 0")
    @DecimalMax(value = "10.0", inclusive = true, message = "Rating must be <= 10")
    private Double rating;

    private String genre;
    private String language;

    @NotNull(message = "Status is mandatory")
    private MovieStatus status;
}
