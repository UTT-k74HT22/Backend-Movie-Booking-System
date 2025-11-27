package com.trainning.movie_booking_system.helper.redis;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ConflictException;
import com.trainning.movie_booking_system.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service quản lý hold/release seats sử dụng Redis
 * Đảm bảo atomic operations và tránh race condition
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatDomainService {

    private final StringRedisTemplate redis;
    private final SeatRepository seatRepository;

    private String holdKey(Long showtimeId, Long seatId) {
        return "hold:%d:%d".formatted(showtimeId, seatId);
    }

    /**
     * Hold seats ATOMIC - rollback nếu fail
     */
    public void holdSeats(Long showtimeId, List<Long> seatIds, Long userId, Duration ttl) {
        log.info("[SEAT-HOLD] User {} attempting to hold seats {} for showtime {}", userId, seatIds, showtimeId);
        List<Long> heldSeats = new ArrayList<>();
        try {
            for (Long seatId : seatIds) {
                String key = holdKey(showtimeId, seatId);
                String userIdStr = String.valueOf(userId);

                Boolean success = redis.opsForValue().setIfAbsent(key, userIdStr, ttl);
                if (Boolean.TRUE.equals(success)) {
                    heldSeats.add(seatId);
                    log.debug("[SEAT-HOLD] Seat {} held successfully", seatId);
                } else {
                    String currentOwner = redis.opsForValue().get(key);
                    if (userIdStr.equals(currentOwner)) {
                        redis.expire(key, ttl);
                        heldSeats.add(seatId);
                        log.debug("[SEAT-HOLD] Seat {} already held by same user, TTL refreshed", seatId);
                    } else {
                        throw new ConflictException("Ghế %d đang được giữ bởi người khác.".formatted(seatId));
                    }
                }
            }
            log.info("[SEAT-HOLD] Successfully held {} seats for user {}", heldSeats.size(), userId);
        } catch (Exception e) {
            log.error("[SEAT-HOLD] Failed to hold seats, rolling back {} seats", heldSeats.size());
            heldSeats.forEach(seatId -> redis.delete(holdKey(showtimeId, seatId)));
            throw e;
        }
    }

    /**
     * Release held seats
     */
    public void releaseHolds(Long showtimeId, List<Long> seatIds) {
        log.info("[SEAT-HOLD] Releasing hold for {} seats", seatIds.size());
        seatIds.forEach(id -> {
            String key = holdKey(showtimeId, id);
            redis.delete(key);
            log.debug("[SEAT-HOLD] Released hold for seat {}", id);
        });
    }

    /**
     * Verify seats held bởi user
     */
    public void assertHeldByUser(Long showtimeId, List<Long> seatIds, Long userId) {
        for (Long seatId : seatIds) {
            String key = holdKey(showtimeId, seatId);
            String owner = redis.opsForValue().get(key);
            if (owner == null)
                throw new ConflictException("Ghế %d không còn được giữ.".formatted(seatId));
            if (!owner.equals(String.valueOf(userId)))
                throw new ConflictException("Ghế %d đang được giữ bởi người khác.".formatted(seatId));
        }
    }

    /**
     * Sau booking thành công: xoá hold
     */
    public void consumeHoldToBooked(Long showtimeId, List<Long> seatIds) {
        seatIds.forEach(id -> redis.delete(holdKey(showtimeId, id)));
        log.info("[SEAT-HOLD] Released hold for {} seats after booking", seatIds.size());
    }

    /**
     * Lấy thông tin ghế từ DB (seatType)
     */
    public List<SeatInfo> getSeatInfos(List<Long> seatIds) {
        var seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundIds = seats.stream().map(s -> s.getId()).toList();
            List<Long> missingIds = seatIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new BadRequestException("Ghế không tồn tại: %s".formatted(missingIds));
        }
        return seats.stream().map(s -> new SeatInfo(s.getId(), s.getSeatType())).toList();
    }

    /**
     * Kiểm tra ghế có đang bị hold không
     */
    public boolean isSeatHeld(Long showtimeId, Long seatId) {
        return Boolean.TRUE.equals(redis.hasKey(holdKey(showtimeId, seatId)));
    }
}
