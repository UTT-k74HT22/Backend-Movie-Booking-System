package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seats",
        indexes = {
                @Index(name = "idx_seat_screen", columnList = "screen_id"),
                @Index(name = "idx_seat_row_seat", columnList = "row_label, seat_number")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_screen_row_seat", 
                        columnNames = {"screen_id", "row_label", "seat_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends BaseEntity {

    @Column(name = "seat_number", nullable = false)
    private int seatNumber; // Số ghế trong hàng

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel; // Ký hiệu hàng ghế A, B, C...

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType; // Loại ghế: STANDARD, VIP, COUPLE

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatStatus status; // Trạng thái ghế: AVAILABLE, BOOKED, MAINTENANCE


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "screen_id", 
            nullable = false, 
            foreignKey = @ForeignKey(name = "fk_seat_screen")
    )
    private Screen screen; // Mỗi ghế luôn liên kết với một màn hình
}
