package com.trainning.movie_booking_system.dto.request.Screen;

import com.trainning.movie_booking_system.untils.enums.ScreenStatus;
import lombok.Getter;

@Getter
public class UpdateScreenRequest {
    private String name;
    private Integer totalSeats;
    private ScreenStatus status;
    private Long theaterId;
}
