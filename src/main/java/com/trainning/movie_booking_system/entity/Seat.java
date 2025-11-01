package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "seats",
        indexes = {
                @Index(name = "idx_seat_screen_id", columnList = "screen_id"),
                @Index(name = "idx_seat_type", columnList = "seat_type"),
                @Index(name = "idx_seat_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "row_label", nullable = false)
    private String rowLabel; //Khí hiệu hàng ghế A, B, C...

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false)
    private SeatType seatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_seat_screen"))
    private Screen screen;

}
