package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.ValidateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherValidationResult;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.entity.VoucherUsage;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.VoucherMapper;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.UserRepository;
import com.trainning.movie_booking_system.repository.VoucherRepository;
import com.trainning.movie_booking_system.repository.VoucherUsageRepository;
import com.trainning.movie_booking_system.service.IVoucherService;
import com.trainning.movie_booking_system.untils.enums.DiscountType;
import com.trainning.movie_booking_system.untils.enums.VoucherStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Implementation of Voucher Service
 * Handles voucher validation, application, and management
 * 
 * ⚠️ TEMPORARILY DISABLED - Not ready for deployment
 */
//@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherServiceImpl implements IVoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VoucherMapper voucherMapper;

    /**
     * ============================================
     * STEP 1: Validate Voucher (8-step validation)
     * ============================================
     * TODO: Implement 8-step validation logic
     * 1. Check voucher exists
     * 2. Check status is ACTIVE
     * 3. Check date validity
     * 4. Check total usage limit
     * 5. Check user usage limit
     * 6. Check minimum order amount
     * 7. Check applicable scope
     * 8. Calculate discount
     */
    @Override
    @Transactional(readOnly = true)
    public VoucherValidationResult validateVoucher(ValidateVoucherRequest request, Long userId) {
        log.info("Validating voucher: {} for user: {}", request.getVoucherCode(), userId);

        // TODO: Step 1 - Find voucher by code
        // Voucher voucher = voucherRepository.findByCode(request.getVoucherCode())
        //     .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // TODO: Step 2 - Check status is ACTIVE
        // if (voucher.getStatus() != VoucherStatus.ACTIVE) {
        //     return buildInvalidResult("Voucher is not active", request);
        // }

        // TODO: Step 3 - Check date validity
        // LocalDateTime now = LocalDateTime.now();
        // if (now.isBefore(voucher.getValidFrom()) || now.isAfter(voucher.getValidUntil())) {
        //     return buildInvalidResult("Voucher is expired or not yet valid", request);
        // }

        // TODO: Step 4 - Check total usage limit
        // if (voucher.getCurrentUsageCount() >= voucher.getTotalUsageLimit()) {
        //     return buildInvalidResult("Voucher usage limit reached", request);
        // }

        // TODO: Step 5 - Check user usage limit
        // int userUsageCount = voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), userId);
        // if (userUsageCount >= voucher.getUsagePerUser()) {
        //     return buildInvalidResult("You have reached the usage limit for this voucher", request);
        // }

        // TODO: Step 6 - Check minimum order amount
        // if (request.getBookingAmount().compareTo(voucher.getMinOrderAmount()) < 0) {
        //     return buildInvalidResult(
        //         String.format("Minimum order amount is %s", voucher.getMinOrderAmount()),
        //         request
        //     );
        // }

        // TODO: Step 7 - Check applicable scope (movies, theaters, days, time slots)
        // Booking booking = bookingRepository.findById(request.getBookingId())
        //     .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        // if (!isApplicableToBooking(voucher, booking)) {
        //     return buildInvalidResult("Voucher is not applicable to this booking", request);
        // }

        // TODO: Step 8 - Calculate discount based on discount type
        // BigDecimal discountAmount = calculateDiscount(voucher, request.getBookingAmount(), booking);

        // TODO: Build and return success result
        // return VoucherValidationResult.builder()
        //     .isValid(true)
        //     .message("Voucher is valid")
        //     .voucherCode(voucher.getCode())
        //     .voucherName(voucher.getName())
        //     .originalAmount(request.getBookingAmount())
        //     .discountAmount(discountAmount)
        //     .finalAmount(request.getBookingAmount().subtract(discountAmount))
        //     .remainingUsage(voucher.getUsagePerUser() - userUsageCount - 1)
        //     .validUntil(voucher.getValidUntil().toString())
        //     .build();

        throw new UnsupportedOperationException("TODO: Implement validateVoucher method");
    }

    /**
     * ============================================
     * HELPER: Check if voucher is applicable to booking
     * ============================================
     * TODO: Implement scope validation logic
     * - Check applicable movies
     * - Check applicable theaters
     * - Check applicable days of week
     * - Check applicable time slots
     */
    private boolean isApplicableToBooking(Voucher voucher, Booking booking) {
        // TODO: If applicableMovieIds is set, check if booking movie is in the list
        // if (voucher.getApplicableMovieIds() != null && !voucher.getApplicableMovieIds().isEmpty()) {
        //     Long movieId = booking.getShowtime().getMovie().getId();
        //     if (!voucher.getApplicableMovieIds().contains(movieId)) {
        //         return false;
        //     }
        // }

        // TODO: If applicableTheaterIds is set, check if booking theater is in the list
        // if (voucher.getApplicableTheaterIds() != null && !voucher.getApplicableTheaterIds().isEmpty()) {
        //     Long theaterId = booking.getShowtime().getTheater().getId();
        //     if (!voucher.getApplicableTheaterIds().contains(theaterId)) {
        //         return false;
        //     }
        // }

        // TODO: If applicableDaysOfWeek is set, check if booking day matches
        // if (voucher.getApplicableDaysOfWeek() != null && !voucher.getApplicableDaysOfWeek().isEmpty()) {
        //     int dayOfWeek = booking.getShowtime().getStartTime().getDayOfWeek().getValue();
        //     if (!voucher.getApplicableDaysOfWeek().contains(dayOfWeek)) {
        //         return false;
        //     }
        // }

        // TODO: If applicableTimeSlots is set, check if booking time is within slots
        // if (voucher.getApplicableTimeSlots() != null && !voucher.getApplicableTimeSlots().isEmpty()) {
        //     LocalTime showtimeTime = booking.getShowtime().getStartTime().toLocalTime();
        //     if (!isTimeInSlots(showtimeTime, voucher.getApplicableTimeSlots())) {
        //         return false;
        //     }
        // }

        throw new UnsupportedOperationException("TODO: Implement isApplicableToBooking method");
    }

    /**
     * ============================================
     * HELPER: Check if time is within any of the time slots
     * ============================================
     * TODO: Parse time slot strings (e.g., "10:00-12:00") and check if time is within range
     */
    private boolean isTimeInSlots(LocalTime time, java.util.List<String> timeSlots) {
        // TODO: Parse each time slot and check if time falls within range
        // for (String slot : timeSlots) {
        //     String[] parts = slot.split("-");
        //     LocalTime start = LocalTime.parse(parts[0]);
        //     LocalTime end = LocalTime.parse(parts[1]);
        //     if (!time.isBefore(start) && !time.isAfter(end)) {
        //         return true;
        //     }
        // }
        // return false;

        throw new UnsupportedOperationException("TODO: Implement isTimeInSlots method");
    }

    /**
     * ============================================
     * HELPER: Calculate discount based on discount type
     * ============================================
     * TODO: Implement 4 discount type calculations:
     * - PERCENTAGE: amount * (discountValue / 100)
     * - FIXED_AMOUNT: discountValue
     * - BUY_X_GET_Y: (get quantity / buy quantity) * ticket price
     * - FREE_SHIPPING: 0 (not applicable for movie booking)
     */
    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal bookingAmount, Booking booking) {
        BigDecimal discountAmount = BigDecimal.ZERO;

        // TODO: Implement discount calculation based on type
        // switch (voucher.getDiscountType()) {
        //     case PERCENTAGE:
        //         // Calculate percentage discount
        //         discountAmount = bookingAmount
        //             .multiply(voucher.getDiscountValue())
        //             .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        //         break;
        //
        //     case FIXED_AMOUNT:
        //         // Fixed amount discount
        //         discountAmount = voucher.getDiscountValue();
        //         break;
        //
        //     case BUY_X_GET_Y:
        //         // Buy X tickets get Y tickets free
        //         // Calculate based on booking tickets quantity
        //         int totalTickets = booking.getTickets().size();
        //         int buyQty = voucher.getBuyQuantity();
        //         int getQty = voucher.getGetQuantity();
        //         int freeTickets = (totalTickets / buyQty) * getQty;
        //         // Calculate average ticket price
        //         BigDecimal avgTicketPrice = bookingAmount.divide(
        //             BigDecimal.valueOf(totalTickets), 2, RoundingMode.HALF_UP
        //         );
        //         discountAmount = avgTicketPrice.multiply(BigDecimal.valueOf(freeTickets));
        //         break;
        //
        //     case FREE_SHIPPING:
        //         // Not applicable for movie booking
        //         discountAmount = BigDecimal.ZERO;
        //         break;
        //
        //     default:
        //         throw new BadRequestException("Unknown discount type");
        // }

        // TODO: Apply maximum discount limit if set
        // if (voucher.getMaxDiscountAmount() != null &&
        //     discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
        //     discountAmount = voucher.getMaxDiscountAmount();
        // }

        // TODO: Ensure discount doesn't exceed booking amount
        // if (discountAmount.compareTo(bookingAmount) > 0) {
        //     discountAmount = bookingAmount;
        // }

        // return discountAmount.setScale(2, RoundingMode.HALF_UP);

        throw new UnsupportedOperationException("TODO: Implement calculateDiscount method");
    }

    /**
     * ============================================
     * HELPER: Build invalid validation result
     * ============================================
     */
    private VoucherValidationResult buildInvalidResult(String message, ValidateVoucherRequest request) {
        return VoucherValidationResult.builder()
                .isValid(false)
                .message(message)
                .voucherCode(request.getVoucherCode())
                .originalAmount(request.getBookingAmount())
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(request.getBookingAmount())
                .build();
    }

    /**
     * ============================================
     * STEP 2: Apply Voucher to Booking
     * ============================================
     * TODO: Create VoucherUsage record and increment usage count
     */
    @Override
    public VoucherUsageResponse applyVoucher(String voucherCode, Long bookingId, Long userId, BigDecimal discountAmount) {
        log.info("Applying voucher {} to booking {} for user {}", voucherCode, bookingId, userId);

        // TODO: Find voucher, booking, and user
        // Voucher voucher = voucherRepository.findByCode(voucherCode)
        //     .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));
        // Booking booking = bookingRepository.findById(bookingId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        // User user = userRepository.findById(userId)
        //     .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // TODO: Create VoucherUsage record
        // VoucherUsage voucherUsage = VoucherUsage.builder()
        //     .voucher(voucher)
        //     .user(user)
        //     .booking(booking)
        //     .originalAmount(booking.getTotalAmount())
        //     .discountAmount(discountAmount)
        //     .finalAmount(booking.getTotalAmount().subtract(discountAmount))
        //     .usedAt(LocalDateTime.now())
        //     .build();
        // voucherUsageRepository.save(voucherUsage);

        // TODO: Increment voucher usage count
        // voucher.setCurrentUsageCount(voucher.getCurrentUsageCount() + 1);
        // voucherRepository.save(voucher);

        // TODO: Map and return response
        // return voucherMapper.toUsageResponse(voucherUsage);

        throw new UnsupportedOperationException("TODO: Implement applyVoucher method");
    }

    /**
     * ============================================
     * STEP 3: Refund Voucher (when booking cancelled)
     * ============================================
     * TODO: Delete VoucherUsage and decrement usage count
     */
    @Override
    public void refundVoucher(Long bookingId) {
        log.info("Refunding voucher for booking {}", bookingId);

        // TODO: Find voucher usage by booking
        // VoucherUsage voucherUsage = voucherUsageRepository.findByBookingId(bookingId)
        //     .orElse(null);
        // if (voucherUsage == null) {
        //     log.info("No voucher usage found for booking {}", bookingId);
        //     return;
        // }

        // TODO: Decrement voucher usage count
        // Voucher voucher = voucherUsage.getVoucher();
        // voucher.setCurrentUsageCount(voucher.getCurrentUsageCount() - 1);
        // voucherRepository.save(voucher);

        // TODO: Delete voucher usage record
        // voucherUsageRepository.deleteByBookingId(bookingId);

        // log.info("Voucher refunded successfully for booking {}", bookingId);

        throw new UnsupportedOperationException("TODO: Implement refundVoucher method");
    }

    /**
     * ============================================
     * STEP 4: Get Public Vouchers
     * ============================================
     * TODO: Return all active public vouchers
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getPublicVouchers(Pageable pageable) {
        log.info("Getting public vouchers");

        // TODO: Get active public vouchers
        // Page<Voucher> vouchers = voucherRepository.findActivePublicVouchers(
        //     LocalDateTime.now(),
        //     pageable
        // );

        // TODO: Map to response and set canUse flag
        // return vouchers.map(voucher -> {
        //     VoucherResponse response = voucherMapper.toResponse(voucher);
        //     response.setCanUse(
        //         voucher.getCurrentUsageCount() < voucher.getTotalUsageLimit()
        //     );
        //     return response;
        // });

        throw new UnsupportedOperationException("TODO: Implement getPublicVouchers method");
    }

    /**
     * ============================================
     * STEP 5: Get User Voucher Usage History
     * ============================================
     * TODO: Return user's voucher usage history
     */
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherUsageResponse> getUserVoucherUsageHistory(Long userId, Pageable pageable) {
        log.info("Getting voucher usage history for user {}", userId);

        // TODO: Find user voucher usages
        // Page<VoucherUsage> usages = voucherUsageRepository.findByUserId(userId, pageable);

        // TODO: Map to response
        // return usages.map(voucherMapper::toUsageResponse);

        throw new UnsupportedOperationException("TODO: Implement getUserVoucherUsageHistory method");
    }

    /**
     * ============================================
     * ADMIN OPERATIONS
     * ============================================
     */

    @Override
    public VoucherResponse createVoucher(CreateVoucherRequest request) {
        log.info("Creating voucher with code: {}", request.getCode());

        // TODO: Check if voucher code already exists
        // if (voucherRepository.existsByCode(request.getCode())) {
        //     throw new BadRequestException("Voucher code already exists");
        // }

        // TODO: Validate date range
        // if (request.getValidFrom().isAfter(request.getValidUntil())) {
        //     throw new BadRequestException("Valid from date must be before valid until date");
        // }

        // TODO: Validate BUY_X_GET_Y specific fields
        // if (request.getDiscountType() == DiscountType.BUY_X_GET_Y) {
        //     if (request.getBuyQuantity() == null || request.getGetQuantity() == null) {
        //         throw new BadRequestException("Buy quantity and get quantity are required for BUY_X_GET_Y type");
        //     }
        // }

        // TODO: Map to entity and save
        // Voucher voucher = voucherMapper.toEntity(request);
        // voucher = voucherRepository.save(voucher);

        // TODO: Map and return response
        // return voucherMapper.toResponse(voucher);

        throw new UnsupportedOperationException("TODO: Implement createVoucher method");
    }

    @Override
    public VoucherResponse updateVoucher(Long voucherId, UpdateVoucherRequest request) {
        log.info("Updating voucher {}", voucherId);

        // TODO: Find voucher
        // Voucher voucher = voucherRepository.findById(voucherId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // TODO: Validate date range if both dates are provided
        // if (request.getValidFrom() != null && request.getValidUntil() != null) {
        //     if (request.getValidFrom().isAfter(request.getValidUntil())) {
        //         throw new BadRequestException("Valid from date must be before valid until date");
        //     }
        // }

        // TODO: Update entity using mapper
        // voucherMapper.updateEntityFromRequest(request, voucher);
        // voucher = voucherRepository.save(voucher);

        // TODO: Map and return response
        // return voucherMapper.toResponse(voucher);

        throw new UnsupportedOperationException("TODO: Implement updateVoucher method");
    }

    @Override
    public void deleteVoucher(Long voucherId) {
        log.info("Deleting voucher {}", voucherId);

        // TODO: Find voucher
        // Voucher voucher = voucherRepository.findById(voucherId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // TODO: Soft delete by setting status to INACTIVE
        // voucher.setStatus(VoucherStatus.INACTIVE);
        // voucherRepository.save(voucher);

        throw new UnsupportedOperationException("TODO: Implement deleteVoucher method");
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(Long voucherId) {
        log.info("Getting voucher {}", voucherId);

        // TODO: Find voucher
        // Voucher voucher = voucherRepository.findById(voucherId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Voucher not found"));

        // TODO: Map and return response
        // return voucherMapper.toResponse(voucher);

        throw new UnsupportedOperationException("TODO: Implement getVoucherById method");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(Pageable pageable) {
        log.info("Getting all vouchers");

        // TODO: Get all vouchers (admin view)
        // Page<Voucher> vouchers = voucherRepository.findAll(pageable);

        // TODO: Map to response
        // return vouchers.map(voucherMapper::toResponse);

        throw new UnsupportedOperationException("TODO: Implement getAllVouchers method");
    }

    /**
     * ============================================
     * SCHEDULED TASK: Deactivate Expired Vouchers
     * ============================================
     * TODO: Auto-deactivate expired vouchers (runs daily)
     */
    @Override
    public void deactivateExpiredVouchers() {
        log.info("Deactivating expired vouchers");

        // TODO: Find all expired vouchers
        // java.util.List<Voucher> expiredVouchers = voucherRepository.findExpiredVouchers(LocalDateTime.now());

        // TODO: Set status to INACTIVE and save
        // for (Voucher voucher : expiredVouchers) {
        //     voucher.setStatus(VoucherStatus.INACTIVE);
        // }
        // voucherRepository.saveAll(expiredVouchers);

        // log.info("Deactivated {} expired vouchers", expiredVouchers.size());

        throw new UnsupportedOperationException("TODO: Implement deactivateExpiredVouchers method");
    }
}
