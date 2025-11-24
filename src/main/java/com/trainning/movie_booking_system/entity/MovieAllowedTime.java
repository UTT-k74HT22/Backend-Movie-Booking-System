package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "movie_allowed_times")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieAllowedTime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;
}
