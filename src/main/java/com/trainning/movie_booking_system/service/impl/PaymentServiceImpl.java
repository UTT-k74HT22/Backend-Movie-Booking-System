package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.repository.PaymentTransactionRepository;
import com.trainning.movie_booking_system.service.PaymentService;
import com.trainning.movie_booking_system.service.VnPayService;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final SeatDomainService seatDomainService;
    private final VnPayService vnPayService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Override
    @Transactional
    public String createPaymentUrl(Long bookingId) {
        log.info("[PAYMENT] Creating payment URL for booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                    "Booking is not in PENDING_PAYMENT status. Current status: " + booking.getStatus()
            );
        }

        String txnRef = "TXN_" + System.currentTimeMillis() + "_" + bookingId;

        // Tạo transaction với finalAmount
        var paymentTransaction = com.trainning.movie_booking_system.entity.PaymentTransaction.builder()
                .booking(booking)
                .gatewayType(com.trainning.movie_booking_system.untils.enums.PaymentGatewayType.VNPAY)
                .transactionId(txnRef)
                .amount(booking.getTotalPrice())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount()) // đúng số đã giảm
                .currency("VND")
                .status(com.trainning.movie_booking_system.untils.enums.PaymentStatus.PENDING)
                .ipAddress("127.0.0.1")
                .initiatedAt(java.time.LocalDateTime.now())
                .build();

        paymentTransactionRepository.save(paymentTransaction);
        log.info("[PAYMENT] Created payment transaction: {}", txnRef);

        // Gửi VNPay: VNPay tính số tiền *100
        // finalAmount đã là VND, chỉ cần làm tròn về số nguyên
        long amountVnd = booking.getFinalAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        String orderInfo = "Thanh toan ve xem phim - Booking #" + bookingId;
        String clientIp = "127.0.0.1";

        String paymentUrl = vnPayService.createPaymentUrl(txnRef, amountVnd, orderInfo, clientIp);
        log.info("[PAYMENT] Payment URL created successfully for booking {}", bookingId);

        return paymentUrl;
    }

    @Override
    @Transactional
    public PaymentResponse handleVNPayReturn(jakarta.servlet.http.HttpServletRequest request) {
        log.info("[PAYMENT] Processing VNPay return callback");

        int paymentStatus = vnPayService.orderReturn(request);
        String vnpTxnRef = request.getParameter("vnp_TxnRef");
        String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
        String vnpBankCode = request.getParameter("vnp_BankCode");

        com.trainning.movie_booking_system.entity.PaymentTransaction transaction =
                paymentTransactionRepository.findByTransactionId(vnpTxnRef)
                        .orElseThrow(() -> new NotFoundException("Transaction not found: " + vnpTxnRef));

        Booking booking = transaction.getBooking();

        if (paymentStatus == 1) {
            transaction.setStatus(com.trainning.movie_booking_system.untils.enums.PaymentStatus.SUCCESS);
            transaction.setGatewayOrderId(vnpTransactionNo);
            transaction.setPaymentMethod(vnpBankCode);
            transaction.setCompletedAt(java.time.LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.consumeHoldToBooked(booking.getShowtime().getId(), seatIds);

            log.info("[PAYMENT]  Payment SUCCESS for booking {}", booking.getId());

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("SUCCESS")
                    .message("Payment completed successfully")
                    .build();

        } else {
            transaction.setStatus(com.trainning.movie_booking_system.untils.enums.PaymentStatus.FAILED);
            transaction.setCompletedAt(java.time.LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

            log.warn("[PAYMENT]  Payment FAILED for booking {}", booking.getId());

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("FAILED")
                    .message("Payment failed or cancelled")
                    .build();
        }
    }

    @Override
    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + request.getBookingId()));

        if ("SUCCESS".equals(request.getStatus())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.consumeHoldToBooked(booking.getShowtime().getId(), seatIds);

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("SUCCESS")
                    .message("Payment completed successfully")
                    .build();

        } else {
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("FAILED")
                    .message("Payment failed or cancelled")
                    .build();
        }
    }

    @Override
    public String verifyPaymentStatus(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        return booking.getStatus().name();
    }

    @Override
    @Transactional
    public void cancelPayment(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel confirmed booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            return;
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
        seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);
    }
}
