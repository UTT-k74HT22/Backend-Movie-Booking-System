package com.trainning.movie_booking_system.helper.cron;

import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.BookingSeatRepository;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingExpireService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;

    /**
     * Cron job sẽ chạy mỗi 10 phút để kiểm tra các booking đã hết hạn.
     */
    @Transactional
    @Scheduled(cron = "0 */10 * * * *")  // Chạy mỗi 10 phút
    public void expireBookings() {
        log.info("[BOOKING EXPIRE] Checking expired bookings...");

        // Lấy danh sách booking PENDING_PAYMENT quá 15 phút
        List<Booking> expiredBookings = bookingRepository.findAllExpiredBookings(
                BookingStatus.PENDING_PAYMENT,
                LocalDateTime.now().minusMinutes(15)
        );

        for (Booking booking : expiredBookings) {
            // Cập nhật trạng thái booking
            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);

            // Xóa booking_seat để giải phóng ghế
            bookingSeatRepository.deleteByBookingId(booking.getId());

            log.info("[BOOKING EXPIRE] Booking ID {} has been marked as EXPIRED and seats released.", booking.getId());
        }
    }
}
