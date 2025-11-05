package com.trainning.movie_booking_system.helper;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class RedisLockService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisLockService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Thử lấy lock từ Redis
     *
     * @param lockKey   Key để lock (ví dụ: "seatLock:{showtimeId}")
     * @param ttl       Thời gian khóa sẽ tồn tại
     * @param timeUnit  Đơn vị thời gian
     * @return true nếu lấy được lock, false nếu không
     */
    public boolean tryLock(String lockKey, long ttl, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", ttl, timeUnit));
    }

    /**
     * Giải phóng lock
     *
     * @param lockKey Key của lock
     */
    public void releaseLock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}

