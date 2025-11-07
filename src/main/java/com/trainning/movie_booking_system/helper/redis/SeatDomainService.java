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
     * Hold seats ATOMIC - dùng SETNX để tránh race condition
     * Nếu fail 1 ghế thì rollback tất cả
     */
    public void holdSeats(Long showtimeId, List<Long> seatIds, Long userId, Duration ttl) {
        log.info("[SEAT-HOLD] User {} attempting to hold seats {} for showtime {}",
                userId, seatIds, showtimeId);

        List<Long> heldSeats = new ArrayList<>();

        try {
            for (Long seatId : seatIds) {
                String key = holdKey(showtimeId, seatId);
                String userIdStr = String.valueOf(userId);

                // ATOMIC: SET if Not eXists
                Boolean success = redis.opsForValue().setIfAbsent(key, userIdStr, ttl);

                if (Boolean.TRUE.equals(success)) {
                    heldSeats.add(seatId);
                    log.debug("[SEAT-HOLD] Seat {} held successfully", seatId);
                } else {
                    // Check nếu chính user này đang hold (idempotent)
                    String currentOwner = redis.opsForValue().get(key);
                    if (userIdStr.equals(currentOwner)) {
                        // Refresh TTL nếu user hold lại
                        redis.expire(key, ttl);
                        heldSeats.add(seatId);
                        log.debug("[SEAT-HOLD] Seat {} already held by same user, refreshed TTL", seatId);
                    } else {
                        // Ghế đang được hold bởi user khác
                        throw new ConflictException(
                            "Ghế %d đang được giữ bởi người khác. Vui lòng chọn ghế khác.".formatted(seatId)
                        );
                    }
                }
            }
            log.info("[SEAT-HOLD] Successfully held {} seats for user {}", heldSeats.size(), userId);
        } catch (Exception e) {
            // Rollback: release tất cả ghế đã hold
            log.error("[SEAT-HOLD] Failed to hold seats, rolling back {} seats", heldSeats.size());
            heldSeats.forEach(seatId -> redis.delete(holdKey(showtimeId, seatId)));
            throw e;
        }
    }

    /**
     * Kiểm tra toàn bộ ghế đang được hold bởi user hiện tại (còn TTL)
     * Phải gọi trước khi create booking để verify
     */
    public void assertHeldByUser(Long showtimeId, List<Long> seatIds, Long userId) {
        log.debug("[SEAT-HOLD] Verifying {} seats held by user {}", seatIds.size(), userId);

        for (Long seatId : seatIds) {
            String key = holdKey(showtimeId, seatId);
            String owner = redis.opsForValue().get(key);

            if (owner == null) {
                throw new ConflictException(
                    "Ghế %d không còn được giữ (hết hạn hold). Vui lòng hold lại.".formatted(seatId)
                );
            }
            if (!owner.equals(String.valueOf(userId))) {
                throw new ConflictException(
                    "Ghế %d đang được giữ bởi người khác. Không thể booking.".formatted(seatId)
                );
            }
        }
    }

    /**
     * Lấy thông tin ghế để tính giá (VIP/STANDARD)
     */
    public List<SeatInfo> getSeatInfos(List<Long> seatIds) {
        var seats = seatRepository.findAllById(seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundIds = seats.stream().map(s -> s.getId()).toList();
            List<Long> missingIds = seatIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
            throw new BadRequestException(
                "Ghế không tồn tại: %s".formatted(missingIds)
            );
        }
        return seats.stream()
            .map(s -> new SeatInfo(s.getId(), s.getSeatType()))
            .toList();
    }

    /**
     * Sau thanh toán thành công: xoá hold
     * Ghế đã được persist vào DB (booking_seats table)
     */
    public void consumeHoldToBooked(Long showtimeId, List<Long> seatIds) {
        log.info("[SEAT-HOLD] Consuming hold to booked for {} seats", seatIds.size());
        seatIds.forEach(id -> {
            String key = holdKey(showtimeId, id);
            redis.delete(key);
            log.debug("[SEAT-HOLD] Released hold for seat {} (now booked)", id);
        });
    }

    /**
     * Khi thanh toán fail/timeout/user cancel: xoá hold để nhả ghế
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
     * Check xem ghế có đang bị hold không (bất kể user nào)
     */
    public boolean isSeatHeld(Long showtimeId, Long seatId) {
        String key = holdKey(showtimeId, seatId);
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

}
