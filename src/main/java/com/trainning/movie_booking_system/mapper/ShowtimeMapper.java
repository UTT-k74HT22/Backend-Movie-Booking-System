package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.entity.Showtime;

public class ShowtimeMapper {

    public static ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movieId(showtime.getMovieId())
                .screen(ScreenMapper.toScreenResponse(showtime.getScreen()))
                .showDate(showtime.getShowDate())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .price(showtime.getPrice())
                .status(showtime.getStatus())
                .build();
    }
}
