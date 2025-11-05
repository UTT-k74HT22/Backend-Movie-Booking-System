package com.trainning.movie_booking_system.helper;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SeatClient {
    // For now mock. When seat module done, replace with Feign or service call.

    public List<SeatInfo> getSeatInfos(Long showtimeId, List<Long> seatIds) {
        // simple mock: if seatId % 10 == 0 -> VIP else STANDARD
        return seatIds.stream().map(id -> {
            SeatInfo info = new SeatInfo();
            info.setSeatId(id);
            info.setSeatType(id % 10 == 0 ? SeatType.VIP : SeatType.STANDARD);
            return info;
        }).collect(Collectors.toList());
    }

    public void validateSeatsAvailable(Long showtimeId, List<Long> seatIds) {
        // For now assume available. Later call seat-service to check status + holds.
    }
}
