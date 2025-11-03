package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "booking_seats",
        indexes = {
                @Index(name = "idx_booking", columnList = "booking_id"),
                @Index(name = "idx_seat", columnList = "seat_id"),
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    private Long seatId;

    private BigDecimal price;
}
