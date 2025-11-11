package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.SeatInfo;
import com.trainning.movie_booking_system.dto.request.Booking.BookingRequest;
import com.trainning.movie_booking_system.dto.response.Booking.BookingResponse;
import com.trainning.movie_booking_system.dto.response.System.PageResponse;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.BookingSeat;
import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.entity.Showtime;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.ConflictException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.RedisLockService;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.BookingSeatRepository;
import com.trainning.movie_booking_system.repository.SeatRepository;
import com.trainning.movie_booking_system.repository.ShowtimeRepository;
import com.trainning.movie_booking_system.security.SecurityUtils;
import com.trainning.movie_booking_system.service.BookingService;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import com.trainning.movie_booking_system.untils.enums.SeatType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import static com.trainning.movie_booking_system.mapper.BookingMapper.toResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final ShowtimeRepository showtimeRepository;
    private final RedisLockService redisLockService;
    private final SeatDomainService seatClient;

    /**
     * Create a new booking
     * Flow:
     * 1. Validate input & showtime exists
     * 2. Pre-check: Verify seats held by current user
     * 3. Get seat infos for price calculation
     * 4. Acquire distributed locks (sorted order to prevent deadlock)
     * 5. Re-verify holds under lock (TOCTOU prevention)
     * 6. Create booking transaction in DB
     * 7. Consume Redis holds (seats now persisted in DB)
     * 8. Release locks in finally block
     *
     * @param request the booking request data
     * @return the created booking response
     */
    @Override
    public BookingResponse create(BookingRequest request) {
        log.info("[BOOKING] Create booking request: {}", request);

        var currentUser = SecurityUtils.getCurrentUserDetails();
        Long userId = currentUser.getAccount().getId();

        // ===== 1. Validate input =====
        if (CollectionUtils.isEmpty(request.getSeatIds())) {
            throw new BadRequestException("Seat list must not be empty");
        }

        // ===== 2. Validate showtime exists =====
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new NotFoundException("Showtime not found with ID: " + request.getShowtimeId()));
        validateShowTime(showtime);

        // ===== 3. Verify seats are held by current user (pre-check) =====
        seatClient.assertHeldByUser(request.getShowtimeId(), request.getSeatIds(), userId);

        // ===== 4. Get seat infos for price calculation =====
        List<SeatInfo> seatInfos = seatClient.getSeatInfos(request.getSeatIds());

        // ===== 5. Lock từng ghế riêng lẻ (tránh deadlock) =====
        // Sort seats để đảm bảo lock order nhất quán
        List<Long> sortedSeatIds = request.getSeatIds().stream().sorted().toList();
        List<Long> lockedSeats = new ArrayList<>();

        try {
            // Acquire locks
            for (Long seatId : sortedSeatIds) {
                if (!redisLockService.tryLockSeat(request.getShowtimeId(), seatId, 30, TimeUnit.SECONDS)) {
                    throw new ConflictException(
                        "Không thể lock ghế %d. Vui lòng thử lại.".formatted(seatId)
                    );
                }
                lockedSeats.add(seatId);
            }

            // ===== 6. Re-verify holds under lock (tránh TOCTOU) =====
            seatClient.assertHeldByUser(request.getShowtimeId(), request.getSeatIds(), userId);

            // ===== 7. Create booking in transaction =====
            BookingResponse response = createBookingTransaction(showtime, seatInfos, request, userId);

            // ===== 8. CRITICAL: Consume hold to booked =====
            seatClient.consumeHoldToBooked(request.getShowtimeId(), request.getSeatIds());

            log.info("[BOOKING] Successfully created booking ID {} for user {}",
                    response.getId(), userId);

            return response;

        } finally {
            // ===== 9. ALWAYS release locks =====
            for (Long seatId : lockedSeats) {
                redisLockService.releaseSeatLock(request.getShowtimeId(), seatId);
            }
            log.debug("[BOOKING] Released {} locks", lockedSeats.size());
        }
    }

    /**
     * Transactional method để persist booking vào DB
     * QUAN TRỌNG: Phải check DB seats đã booked cho ĐÚNG showtime này
     */
    @Transactional
    protected BookingResponse createBookingTransaction(
            Showtime showtime,
            List<SeatInfo> seatInfos,
            BookingRequest request,
            Long userId) {

        // Check ghế đã được booking trong DB cho showtime này
        // (PENDING_PAYMENT hoặc CONFIRMED)
        List<Long> bookedSeats = bookingSeatRepository.findBookedSeatIds(
                request.getShowtimeId(),
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.CONFIRMED),
                request.getSeatIds()
        );
        if (!bookedSeats.isEmpty()) {
            throw new ConflictException(
                "Ghế đã được đặt trong DB: %s. Có thể do race condition.".formatted(bookedSeats)
            );
        }

        // Get account from SecurityContext
        var account = SecurityUtils.getCurrentUserDetails().account();

        var booking = Booking.builder()
                .account(account)
                .showtime(showtime)
                .status(BookingStatus.PENDING_PAYMENT)
                .expiresAt(LocalDateTime.now().plusMinutes(15))  // Expire after 15 minutes
                .build();

        var seatIdToEntity = seatRepository.findAllById(
                seatInfos.stream().map(SeatInfo::getSeatId).toList()
        ).stream().collect(Collectors.toMap(Seat::getId, Function.identity()));

        var bookingSeats = new ArrayList<BookingSeat>();
        BigDecimal total = BigDecimal.ZERO;
        for (SeatInfo info : seatInfos) {
            Seat seat = seatIdToEntity.get(info.getSeatId());
            
            BigDecimal multiplier = (info.getSeatType() == SeatType.VIP) ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
            BigDecimal price = showtime.getPrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

            bookingSeats.add(BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(price)
                    // Copy denormalized seat info for performance (avoid N+1 query)
                    .seatNumber(seat.getSeatNumber())
                    .rowLabel(seat.getRowLabel())
                    .seatType(seat.getSeatType())
                    .build());
            total = total.add(price);
        }
        booking.setTotalPrice(total);

        bookingRepository.save(booking);
        bookingSeatRepository.saveAll(bookingSeats);
        booking.setBookingSeats(bookingSeats);

        return toResponse(booking);
    }

    /**
     * Update an existing booking - NOT SUPPORTED
     * Booking should not be updated after creation
     * User can only cancel via payment cancellation
     *
     * @param id      the ID of the booking to update
     * @param request the updated booking request data
     * @return the updated booking response
     */
    @Override
    public BookingResponse update(Long id, BookingRequest request) {
        throw new UnsupportedOperationException(
            "Booking update is not supported. Please cancel and create a new booking."
        );
    }

    /**
     * Delete a booking by ID - NOT SUPPORTED
     * Booking should not be deleted, only cancelled via payment flow
     *
     * @param id the ID of the booking to delete
     */
    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException(
            "Booking deletion is not supported. Use payment cancellation instead."
        );
    }

    /**
     * Get a booking by ID
     *
     * @param id the ID of the booking to retrieve
     * @return the booking response
     */
    @Override
    public BookingResponse getById(Long id) {
        log.info("[BOOKING] Get booking by id: {}", id);

        var booking = bookingRepository.findByIdWithSeats(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with ID: " + id));

        return toResponse(booking);
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
        log.info("[BOOKING] Get all bookings: page={}, size={}", pageNumber, pageSize);
        
        // TODO: Implement pagination with proper sorting
        // TODO: Add filters (by user, by showtime, by status, by date range)
        
        throw new UnsupportedOperationException("Pagination not yet implemented");
    }

    // ========== PRIVATE METHODS ========== //
    
    /**
     * Validate showtime chưa bắt đầu và còn thời gian đặt vé
     * 
     * @param showtime Showtime cần validate
     * @throws BadRequestException nếu showtime đã qua hoặc vượt cutoff time
     */
    private void validateShowTime(Showtime showtime) {
        LocalDateTime now = LocalDateTime.now();
        
        // Combine show_date + start_time thành LocalDateTime
        LocalDateTime showtimeStart = LocalDateTime.of(
            showtime.getShowDate(),
            showtime.getStartTime()
        );
        
        // Rule 1: Không được book suất chiếu đã qua
        if (showtimeStart.isBefore(now)) {
            log.warn("Attempt to book past showtime: showtimeId={}, showtimeStart={}, now={}", 
                    showtime.getId(), showtimeStart, now);
            throw new BadRequestException(
                String.format("Cannot book for past showtime. Showtime was at %s", 
                    showtimeStart)
            );
        }
        
        // Rule 2: Đóng booking 15 phút trước giờ chiếu
        LocalDateTime cutoffTime = showtimeStart.minusMinutes(15);
        if (now.isAfter(cutoffTime)) {
            log.warn("Attempt to book within cutoff time: showtimeId={}, cutoffTime={}, now={}", 
                    showtime.getId(), cutoffTime, now);
            throw new BadRequestException(
                String.format("Booking closes 15 minutes before showtime. Cutoff time was %s", 
                    cutoffTime)
            );
        }
        
        log.debug("Showtime validation passed for showtime ID: {}", showtime.getId());
    }
}
