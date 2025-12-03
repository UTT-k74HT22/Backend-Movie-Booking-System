package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Showtime.ShowtimeResponse;
import com.trainning.movie_booking_system.entity.Showtime;

public class ShowtimeMapper {

    /**
     * Map Showtime entity to ShowtimeResponse DTO
     * @param showtime the Showtime entity
     * @return the ShowtimeResponse DTO
     */
    public static ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        if (showtime == null) return null;

        return ShowtimeResponse.builder()
                .id(showtime.getId())
                .movie(MovieMapper.toMovieResponse(showtime.getMovie()))
                .screen(showtime.getScreen() != null ? ScreenMapper.toScreenResponse(showtime.getScreen()) : null)
                .theater(showtime.getScreen() != null && showtime.getScreen().getTheater() != null
                        ? TheaterMapper.toTheaterResponse(showtime.getScreen().getTheater()) : null)
                .showDate(showtime.getShowDate() != null ? showtime.getShowDate(): null)
                .startTime(showtime.getStartTime() != null ? showtime.getStartTime(): null)
                .endTime(showtime.getEndTime() != null ? showtime.getEndTime(): null)
                .price(showtime.getPrice())
                .status(showtime.getStatus())
                .build();
    }
}
