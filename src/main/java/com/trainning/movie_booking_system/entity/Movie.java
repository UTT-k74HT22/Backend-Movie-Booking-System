package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.untils.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
        name = "movies",
        indexes = {
                @Index(name = "idx_movie_title", columnList = "title"),
                @Index(name = "idx_movie_genre", columnList = "genre"),
                @Index(name = "idx_movie_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer duration; // phút

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(length = 100)
    private String language;

    @Column(length = 100)
    private String genre;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private MovieStatus status;
}
