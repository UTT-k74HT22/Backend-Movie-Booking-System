package com.trainning.movie_booking_system.dto.request.Seat;

import com.trainning.movie_booking_system.untils.enums.SeatStatus;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatRequest {
    @NotNull(message = "Screen ID cannot be null")
    private Long screenId;

    @NotNull(message = "Seat number cannot be null")
    private String seatNumber;

    @NotNull
    private String rowLabel;

    private SeatType seatType;

    private SeatStatus status;
}
