package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.BookingSeat;
import com.trainning.movie_booking_system.entity.Showtime;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ConflictException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.RedisLockService;
import com.trainning.movie_booking_system.helper.SeatClient;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.BookingSeatRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.security.SecurityUtils;
import com.trainning.movie_booking_system.service.BookingService;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.trainning.movie_booking_system.mapper.BookingMapper.toResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingSeatRepository bookingSeatRepository;
    private final SeatClient seatClient;
    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final RedisLockService redisLockService;

    /**
     * Create a new booking
     *
     * @param request the booking request data
     * @return the created booking response
     */
    @Override
    public BookingResponse create(BookingRequest request) {
        log.info("[BOOKING] Create booking request: {}", request);

        // ===== 1 Validate input =====
        if (CollectionUtils.isEmpty(request.getSeatIds())) {
            throw new BadRequestException("Seat list must not be empty");
        }

        // ===== 2 Call external service (ngoài transaction) =====
        var showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found with ID: " + request.getShowtimeId()));

        // Kiểm tra seat có tồn tại và chưa bị hold hết hạn
        seatClient.validateSeatsAvailable(request.getShowtimeId(), request.getSeatIds());
        var seatInfos = seatClient.getSeatInfos(request.getShowtimeId(), request.getSeatIds());

        // ===== 3 Lock theo cụm seat =====
        String seatKeyGroup = request.getSeatIds().stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String lockKey = "seatLock:" + request.getShowtimeId() + ":" + seatKeyGroup;

        if (!redisLockService.tryLock(lockKey, 30, TimeUnit.SECONDS)) {
            log.warn("[BOOKING] Lock not acquired for showtime {} seats {}",
                    request.getShowtimeId(), request.getSeatIds());
            throw new ConflictException("System is busy, please try again later.");
        }

        try {
            // ===== 4 Thực hiện booking trong transaction =====
            return createBookingTransaction(showtime, seatInfos, request);
        } finally {
            redisLockService.releaseLock(lockKey);
        }
    }

    // Transactional phần này riêng biệt
    @Transactional
    protected BookingResponse createBookingTransaction(Showtime showtime, List<SeatInfo> seatInfos, BookingRequest request) {

        // Check ghế đã bị book bởi người khác chưa (CONFIRMED / PENDING)
        List<Long> bookedSeats = bookingRepository.findBookedSeatIds(
                request.getShowtimeId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT),
                request.getSeatIds());

        if (!bookedSeats.isEmpty()) {
            throw new BadRequestException("Seats already booked: " + bookedSeats);
        }

        // Build booking và seat detail
        var booking = Booking.builder()
                .account(SecurityUtils.getCurrentUserDetails().account())
                .showtime(showtime)
                .status(BookingStatus.PENDING_PAYMENT)
                .build();

        var bookingSeats = buildBookingSeats(booking, seatInfos, showtime.getPrice());
        booking.setTotalPrice(
                bookingSeats.stream()
                        .map(BookingSeat::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        bookingRepository.save(booking);
        bookingSeatRepository.saveAll(bookingSeats);

        log.info("[BOOKING] Booking created with ID={}, totalPrice={}",
                booking.getId(), booking.getTotalPrice());
        return toResponse(booking);
    }

    private List<BookingSeat> buildBookingSeats(Booking booking, List<SeatInfo> seatInfos, BigDecimal basePrice) {
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (SeatInfo info : seatInfos) {
            BigDecimal multiplier = switch (info.getSeatType()) {
                case VIP -> BigDecimal.valueOf(1.3);
                default -> BigDecimal.ONE;
            };
            BigDecimal seatPrice = basePrice.multiply(multiplier)
                    .setScale(2, RoundingMode.HALF_UP);
            bookingSeats.add(new BookingSeat(booking, info.getSeatId(), seatPrice));
        }
        return bookingSeats;
    }

    /**
     * Update an existing booking
     *
     * @param id      the ID of the booking to update
     * @param request the updated booking request data
     * @return the updated booking response
     */
    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        return null;
    }

    /**
     * Delete a booking by ID
     *
     * @param id the ID of the booking to delete
     */
    @Override
    public void delete(Long id) {

    }

    /**
     * Get a booking by ID
     *
     * @param id the ID of the booking to retrieve
     * @return the booking response
     */
    @Override
    public BookingResponse getById(Long id) {
        return null;
    }

    /**
     * Get all bookings with pagination
     *
     * @param pageNumber the page number to retrieve
     * @param pageSize   the number of bookings per page
     * @return a paginated response of bookings
     */
    @Override
    public PageResponse<?> getAlls(int pageNumber, int pageSize) {
        return null;
    }
}
