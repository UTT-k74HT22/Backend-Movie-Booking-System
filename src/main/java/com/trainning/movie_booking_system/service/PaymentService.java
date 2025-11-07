package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.dto.request.Payment.PaymentRequest;
import com.trainning.movie_booking_system.dto.response.Payment.PaymentResponse;

/**
 * Service xử lý payment cho booking
 * TODO: Triển khai integration với Payment Gateway (VNPay, MoMo, Stripe, etc.)
 */
public interface PaymentService {

    /**
     * Tạo payment URL để redirect user sang Payment Gateway
     *
     * @param bookingId ID của booking cần thanh toán
     * @return Payment URL để redirect
     * TODO: Implement payment gateway integration
     */
    String createPaymentUrl(Long bookingId);

    /**
     * Xử lý callback từ Payment Gateway sau khi user thanh toán
     *
     * @param request Payment callback data từ gateway
     * @return Payment response với trạng thái
     * TODO: Verify signature, update booking status
     */
    PaymentResponse handlePaymentCallback(PaymentRequest request);

    /**
     * Verify payment status từ Payment Gateway
     *
     * @param bookingId ID của booking
     * @return Payment status
     * TODO: Query payment gateway để check status
     */
    String verifyPaymentStatus(Long bookingId);

    /**
     * Cancel payment và update booking status
     *
     * @param bookingId ID của booking
     * TODO: Handle payment cancellation, release seats
     */
    void cancelPayment(Long bookingId);
}

