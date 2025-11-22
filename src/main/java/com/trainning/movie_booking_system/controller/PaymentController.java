package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Controller - Xử lý payment flow
 * 
 * ⚠️ TODO: PAYMENT GATEWAY INTEGRATION
 * ===========================================
 * 1. Chọn payment gateway: VNPay / MoMo / Stripe / PayPal
 * 2. Implement createPaymentUrl() với gateway SDK
 * 3. Implement signature verification trong paymentCallback()
 * 4. Add webhook endpoint từ gateway
 * 5. Handle timeout & retry mechanism
 * 6. Add payment transaction logging
 * 7. Integrate với email service để gửi confirmation
 * 
 * Priority: HIGH
 * Assignee: [DEV_NAME]
 * Deadline: [DATE]
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Tạo payment URL và redirect user sang Payment Gateway
     * User gọi sau khi tạo booking thành công (status = PENDING_PAYMENT)
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     * POST /api/v1/bookings/{bookingId}/payment
     * 
     * ⚠️ TODO: Implement real payment gateway integration
     * - Generate signed payment request
     * - Return actual gateway URL
     * - Handle gateway errors
     *
     * @param bookingId ID của booking cần thanh toán
     * @return Payment URL từ gateway
     */
    @PostMapping("/bookings/{bookingId}/payment")
    public ResponseEntity<?> createPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT-CONTROLLER] Create payment for booking {}", bookingId);

        String paymentUrl = paymentService.createPaymentUrl(bookingId);

        return ResponseEntity.ok(BaseResponse.success(
                paymentUrl, 
                "Redirect to payment gateway. Complete payment within 15 minutes."
        ));
    }

    /**
     * VNPay return callback endpoint (LOCAL TESTING MODE)
     * VNPay sẽ redirect user về đây sau khi thanh toán (Success hoặc Fail)
     * 
     * PUBLIC - No authentication (verified by VNPay signature)
     * GET /api/v1/payments/vnpay/callback?vnp_Amount=...&vnp_TxnRef=...&vnp_SecureHash=...
     * 
     * ⚠️ CHÚ Ý: Trong production với frontend, đổi vnpay.returnUrl sang frontend URL
     * Ví dụ: https://yourfrontend.com/payment/result
     * Frontend sẽ nhận params và gọi /vnpay/verify để xử lý
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<?> handleVNPayReturn(jakarta.servlet.http.HttpServletRequest request) {
        log.info("[PAYMENT-CONTROLLER] VNPay return callback received (LOCAL MODE)");
        
        var response = paymentService.handleVNPayReturn(request);
        
        // TODO: Redirect về frontend success/failure page thay vì return JSON
        // For now, return JSON response for testing
        return ResponseEntity.ok(BaseResponse.success(
                response,
                "Payment " + response.getStatus().toLowerCase()
        ));
    }

    /**
     * VNPay verify endpoint (PRODUCTION MODE - Frontend gọi)
     * Frontend nhận params từ VNPay redirect, forward sang đây để verify
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     * POST /api/v1/payments/vnpay/verify
     * Body: Forward all query params from VNPay as request params
     * 
     * Usage:
     * 1. VNPay redirect về: https://yourfrontend.com/payment/result?vnp_Amount=...
     * 2. Frontend extract params, gọi API này
     * 3. Backend verify signature, update booking
     * 4. Frontend hiển thị success/failure
     */
    @PostMapping("/vnpay/verify")
    public ResponseEntity<?> verifyVNPayPayment(jakarta.servlet.http.HttpServletRequest request) {
        log.info("[PAYMENT-CONTROLLER] VNPay verify from frontend (PRODUCTION MODE)");
        
        var response = paymentService.handleVNPayReturn(request);
        
        return ResponseEntity.ok(BaseResponse.success(
                response,
                "Payment verification completed"
        ));
    }

    /**
     * Payment Gateway webhook endpoint
     * Gateway sẽ gọi endpoint này sau khi user thanh toán
     * 
     * PUBLIC - No authentication (verified by signature)
     * POST /api/v1/payments/webhook
     * 
     * ⚠️ TODO: CRITICAL - Implement signature verification
     * - Verify signature từ gateway (HMAC/RSA)
     * - Validate amount matches booking.totalPrice
     * - Check transaction ID uniqueness
     * - Handle idempotency (prevent double processing)
     * 
     * ⚠️ SECURITY: Endpoint này PHẢI verify signature, nếu không attacker có thể
     * fake payment success và confirm booking miễn phí!
     *
     * @param request Payment callback data từ gateway
     * @return Payment result
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> paymentCallback(@RequestBody @Valid PaymentRequest request) {
        log.info("[PAYMENT-CONTROLLER] Payment callback for booking {}", request.getBookingId());

        // TODO: CRITICAL - Verify signature HERE before processing
        // if (!verifySignature(request)) {
        //     throw new SecurityException("Invalid payment signature");
        // }

        var response = paymentService.handlePaymentCallback(request);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    /**
     * Verify payment status
     * Frontend có thể gọi để check trạng thái payment
     * 
     * REQUIRES AUTHENTICATION - User must be logged in
     * GET /api/v1/bookings/{bookingId}/payment-status
     * 
     * ⚠️ TODO: Query payment gateway API để verify status
     * - Call gateway's transaction query API
     * - Compare with local booking status
     * - Return detailed payment info
     *
     * @param bookingId ID của booking
     * @return Payment status
     */
    @GetMapping("/bookings/{bookingId}/payment-status")
    public ResponseEntity<?> verifyPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT-CONTROLLER] Verify payment for booking {}", bookingId);

        String status = paymentService.verifyPaymentStatus(bookingId);

        return ResponseEntity.ok(BaseResponse.success(
                status,
                "Payment status: " + status
        ));
    }

    /**
     * Cancel payment (user cancel hoặc timeout)
     * User có thể gọi để cancel booking trước khi hết timeout
     *
     * REQUIRES AUTHENTICATION - User must be logged in
     * DELETE /api/v1/bookings/{bookingId}/payment
     *
     * @param bookingId ID của booking
     * @return Success message
     */
    @DeleteMapping("/bookings/{bookingId}/payment")
    public ResponseEntity<?> cancelPayment(@PathVariable Long bookingId) {
        log.info("[PAYMENT-CONTROLLER] Cancel payment for booking {}", bookingId);

        paymentService.cancelPayment(bookingId);

        return ResponseEntity.ok(BaseResponse.success(
                null,
                "Payment cancelled successfully. Seats are now available for other users."
        ));
    }
}