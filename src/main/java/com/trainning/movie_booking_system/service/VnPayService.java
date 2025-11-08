package com.trainning.movie_booking_system.service;

public interface VnPayService {

    /**
     * Create a payment URL for VnPay gateway
     *
     * @param txnRef    Transaction reference
     * @param amountVnd Amount in VND
     * @param orderInfo Order information
     * @param clientIp  Client IP address
     * @return Payment URL as a String
     */
    String createPaymentUrl(String txnRef, long amountVnd, String orderInfo, String clientIp);


}
