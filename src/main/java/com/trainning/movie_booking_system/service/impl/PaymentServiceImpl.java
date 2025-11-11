package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;
import com.trainning.movie_booking_system.entity.Booking;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.helper.redis.SeatDomainService;
import com.trainning.movie_booking_system.repository.BookingRepository;
import com.trainning.movie_booking_system.service.PaymentService;
import com.trainning.movie_booking_system.untils.enums.BookingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Payment Service Implementation
 * 
 * ⚠️ TODO: PAYMENT GATEWAY INTEGRATION - DETAILED IMPLEMENTATION GUIDE
 * ========================================================================
 * 
 * STEP 1: Choose Payment Gateway
 * -------------------------------
 * Vietnam: VNPay, MoMo, ZaloPay, VietQR
 * International: Stripe, PayPal, Razorpay
 * 
 * STEP 2: Add Dependencies (pom.xml)
 * -----------------------------------
 * For VNPay: Manual SDK (vnpay-java-sdk)
 * For Stripe: 
 *   <dependency>
 *     <groupId>com.stripe</groupId>
 *     <artifactId>stripe-java</artifactId>
 *     <version>24.1.0</version>
 *   </dependency>
 * 
 * STEP 3: Configure Gateway Credentials (application.yml)
 * --------------------------------------------------------
 * payment:
 *   gateway: vnpay # or stripe, momo
 *   vnpay:
 *     tmnCode: YOUR_TMN_CODE
 *     hashSecret: YOUR_HASH_SECRET
 *     apiUrl: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
 *     returnUrl: http://your-domain.com/api/payments/callback
 *   stripe:
 *     secretKey: sk_test_xxx
 *     publicKey: pk_test_xxx
 *     webhookSecret: whsec_xxx
 * 
 * STEP 4: Implement createPaymentUrl()
 * -------------------------------------
 * Example for VNPay:
 * 
 * String vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
 * Map<String, String> params = new TreeMap<>();
 * params.put("vnp_Version", "2.1.0");
 * params.put("vnp_Command", "pay");
 * params.put("vnp_TmnCode", tmnCode);
 * params.put("vnp_Amount", String.valueOf(booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue()));
 * params.put("vnp_CurrencyCode", "VND");
 * params.put("vnp_TxnRef", String.valueOf(bookingId));
 * params.put("vnp_OrderInfo", "Thanh toan ve xem phim " + bookingId);
 * params.put("vnp_OrderType", "billpayment");
 * params.put("vnp_Locale", "vn");
 * params.put("vnp_ReturnUrl", returnUrl);
 * params.put("vnp_IpAddr", "127.0.0.1");
 * params.put("vnp_CreateDate", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
 * 
 * // Generate secure hash
 * String signData = params.entrySet().stream()
 *     .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
 *     .collect(Collectors.joining("&"));
 * String secureHash = HmacSHA512(hashSecret, signData);
 * params.put("vnp_SecureHash", secureHash);
 * 
 * return vnpUrl + "?" + params.entrySet().stream()...;
 * 
 * STEP 5: Implement Signature Verification
 * -----------------------------------------
 * public boolean verifySignature(PaymentRequest request) {
 *     String receivedHash = request.getSignature();
 *     String dataToSign = buildSignData(request); // All params except signature
 *     String calculatedHash = HmacSHA512(hashSecret, dataToSign);
 *     return receivedHash.equals(calculatedHash);
 * }
 * 
 * STEP 6: Handle Idempotency
 * ---------------------------
 * - Save transactionId vào Payment entity
 * - Check duplicate transactionId before processing
 * - Use @Transactional với isolation level SERIALIZABLE
 * 
 * STEP 7: Add Webhook Endpoint (for async notifications)
 * -------------------------------------------------------
 * @PostMapping("/webhook")
 * public ResponseEntity<?> webhook(@RequestBody String payload, @RequestHeader("Signature") String signature) {
 *     if (!verifyWebhookSignature(payload, signature)) {
 *         return ResponseEntity.status(401).build();
 *     }
 *     // Process webhook...
 * }
 * 
 * STEP 8: Testing
 * ----------------
 * - Use sandbox/test credentials
 * - Test successful payment flow
 * - Test failed payment flow
 * - Test timeout scenarios
 * - Test signature tampering (security test)
 * - Load test với concurrent payments
 * 
 * References:
 * - VNPay API Docs: https://sandbox.vnpayment.vn/apis/docs/
 * - Stripe Docs: https://stripe.com/docs/api
 * - MoMo Docs: https://developers.momo.vn/
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final BookingRepository bookingRepository;
    private final SeatDomainService seatDomainService;

    /**
     * TODO: Implement VNPay/MoMo/Stripe payment gateway
     * Generate payment URL và redirect user
     * 
     * See detailed implementation guide in class-level JavaDoc above
     */
    @Override
    public String createPaymentUrl(Long bookingId) {
        log.info("[PAYMENT] Creating payment URL for booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BadRequestException(
                "Booking is not in PENDING_PAYMENT status. Current status: " + booking.getStatus()
            );
        }

        // TODO: IMPLEMENT REAL PAYMENT GATEWAY
        // =====================================
        // 1. Get gateway config from application.yml
        // 2. Build payment request với amount, orderId, returnUrl
        // 3. Sign request với secret key (HMAC-SHA512 or RSA)
        // 4. Generate payment URL with signed params
        // 5. Return URL để frontend redirect user

        // MOCK URL - Replace this with real gateway URL
        String mockPaymentUrl = "https://payment-gateway.example.com/checkout?bookingId=" + bookingId 
                + "&amount=" + booking.getTotalPrice()
                + "&currency=VND";

        log.warn("[PAYMENT] [TODO] Using MOCK payment URL. Implement real gateway!");
        log.info("[PAYMENT] Mock URL: {}", mockPaymentUrl);
        
        return mockPaymentUrl;
    }

    /**
     * TODO: Xử lý callback từ Payment Gateway
     * CRITICAL: Verify signature và update booking status
     * 
     * SECURITY WARNING: MUST verify signature before processing!
     * If signature verification is not implemented, attackers can fake payment success!
     */
    @Override
    @Transactional
    public PaymentResponse handlePaymentCallback(PaymentRequest request) {
        log.info("[PAYMENT] Handling payment callback for booking {}", request.getBookingId());

        // TODO: CRITICAL - VERIFY SIGNATURE
        // ==================================
        // if (!verifyPaymentSignature(request)) {
        //     log.error("[PAYMENT] [SECURITY] Invalid payment signature for booking {}", request.getBookingId());
        //     throw new SecurityException("Invalid payment signature");
        // }

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking not found: " + request.getBookingId()));

        // TODO: Check idempotency - prevent double processing
        // ====================================================
        // if (paymentTransactionRepository.existsByTransactionId(request.getTransactionId())) {
        //     log.warn("[PAYMENT] Duplicate transaction ID: {}", request.getTransactionId());
        //     return buildResponse(booking, "DUPLICATE");
        // }

        // TODO: Validate amount matches booking.totalPrice
        // =================================================
        // BigDecimal receivedAmount = new BigDecimal(request.getAmount());
        // if (receivedAmount.compareTo(booking.getTotalPrice()) != 0) {
        //     log.error("[PAYMENT] Amount mismatch! Expected: {}, Received: {}", 
        //         booking.getTotalPrice(), receivedAmount);
        //     throw new BadRequestException("Payment amount mismatch");
        // }

        if ("SUCCESS".equals(request.getStatus())) {
            // Payment thành công
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);

            // Consume Redis hold (ghế đã persist trong DB)
            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.consumeHoldToBooked(booking.getShowtime().getId(), seatIds);

            log.info("[PAYMENT] [SUCCESS] Payment successful for booking {}", booking.getId());

            // TODO: Send confirmation email
            // ==============================
            // emailService.sendBookingConfirmation(booking);

            // TODO: Generate QR code for ticket
            // ==================================
            // String qrCode = qrCodeService.generateBookingQR(booking.getId());

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("SUCCESS")
                    .message("Payment completed successfully")
                    .build();

        } else {
            // Payment failed hoặc cancelled
            booking.setStatus(BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            // Release holds để user khác có thể đặt
            List<Long> seatIds = booking.getBookingSeats().stream()
                    .map(bs -> bs.getSeat().getId())
                    .toList();
            seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

            log.warn("[PAYMENT] [FAILED] Payment failed or cancelled for booking {}", booking.getId());

            return PaymentResponse.builder()
                    .bookingId(booking.getId())
                    .status("FAILED")
                    .message("Payment failed or cancelled")
                    .build();
        }
    }

    /**
     * TODO: Query payment gateway để verify status
     * Call gateway's transaction query API để check actual payment status
     */
    @Override
    public String verifyPaymentStatus(Long bookingId) {
        log.info("[PAYMENT] Verifying payment status for booking {}", bookingId);

        // TODO: CALL GATEWAY API TO VERIFY
        // =================================
        // Example for VNPay:
        // String vnpUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
        // Map<String, String> params = new TreeMap<>();
        // params.put("vnp_RequestId", UUID.randomUUID().toString());
        // params.put("vnp_Version", "2.1.0");
        // params.put("vnp_Command", "querydr");
        // params.put("vnp_TmnCode", tmnCode);
        // params.put("vnp_TxnRef", bookingId.toString());
        // params.put("vnp_OrderInfo", "Verify payment for booking " + bookingId);
        // params.put("vnp_TransactionDate", ...);
        // params.put("vnp_CreateDate", ...);
        // params.put("vnp_IpAddr", "127.0.0.1");
        // 
        // String signData = buildSignData(params);
        // String secureHash = HmacSHA512(hashSecret, signData);
        // params.put("vnp_SecureHash", secureHash);
        // 
        // String response = restTemplate.postForObject(vnpUrl, params, String.class);
        // Parse response and return actual status

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        log.warn("[PAYMENT] [TODO] Returning local status. Should query gateway API!");
        return booking.getStatus().name();
    }

    /**
     * Cancel payment và release seats
     * User có thể cancel trước khi hết timeout (15 phút)
     */
    @Override
    @Transactional
    public void cancelPayment(Long bookingId) {
        log.info("[PAYMENT] Cancelling payment for booking {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel confirmed booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.EXPIRED) {
            log.warn("[PAYMENT] Booking {} already in {} status", bookingId, booking.getStatus());
            return; // Idempotent
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release holds
        List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
        seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);

        log.info("[PAYMENT] Payment cancelled for booking {}. Seats released.", bookingId);
    }
}