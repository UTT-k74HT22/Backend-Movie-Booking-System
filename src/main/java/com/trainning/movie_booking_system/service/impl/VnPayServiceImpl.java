package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.config.VnPayProperties;
import com.trainning.movie_booking_system.helper.VnPay.VnPayHelper;
import com.trainning.movie_booking_system.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VnPayServiceImpl implements VnPayService {

    private final VnPayProperties props;

    /**
     * Create a payment URL for VnPay gateway
     *
     * @param txnRef    Transaction reference
     * @param amountVnd Amount in VND
     * @param orderInfo Order information
     * @param clientIp  Client IP address
     * @return Payment URL as a String
     */
    @Override
    public String createPaymentUrl(String txnRef, long amountVnd, String orderInfo, String clientIp) {
        log.info("Creating Payment URL start");

        Map<String, String> vnp = new HashMap<>();
        vnp.put("vnp_Version", "2.1.0");
        vnp.put("vnp_Command", "pay");
        vnp.put("vnp_TmnCode", props.getTmnCode());
        vnp.put("vnp_Amount", String.valueOf(amountVnd * 100)); // x100
        vnp.put("vnp_CurrCode", "VND");
        vnp.put("vnp_TxnRef", txnRef);
        vnp.put("vnp_OrderInfo", orderInfo);
        vnp.put("vnp_OrderType", "other");
        vnp.put("vnp_Locale", "vn");
        vnp.put("vnp_ReturnUrl", props.getReturnUrl());
        vnp.put("vnp_IpAddr", clientIp);
        String now = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now());
        vnp.put("vnp_CreateDate", now);

        // Ký
        Map<String, String> toSign = new HashMap<>(vnp);
        toSign.remove("vnp_Url");
        String hash = VnPayHelper.secureHash(props.getHashSecret(), toSign);
        toSign.put("vnp_SecureHash", hash);

        String query = VnPayHelper.buildQuery(toSign);
        return props.getPayUrl() + "?" + query;
    }

    /**
     * Verify the integrity of VnPay callback parameters
     *
     * @param params Map of callback parameters from VnPay
     * @return true if the signature is valid, false otherwise
     */
    @Override
    public boolean verify(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            log.warn("Missing vnp_SecureHash in parameters");
            return false;
        }
        Map<String, String> toSign = new HashMap<>(params);
        toSign.remove("vnp_SecureHash");
        toSign.remove("vnp_SecureHashType");

        String calculatedHash = VnPayHelper.secureHash(props.getHashSecret(), toSign);
        boolean isValid = receivedHash.equalsIgnoreCase(calculatedHash);
        if (!isValid) {
            log.warn("Invalid VnPay signature: expected {}, got {}", calculatedHash, receivedHash);
        }
        return isValid;
    }

    /**
     * Process VNPay return callback
     * Simple approach like the demo project
     *
     * @param request HttpServletRequest containing VNPay callback params
     * @return 1 = success, 0 = failed, -1 = invalid signature
     */
    @Override
    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        
        // Extract all params
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnpSecureHash = request.getParameter("vnp_SecureHash");
        
        // Remove hash params for verification
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");
        
        // Verify signature
        if (verify(fields)) {
            // Check transaction status
            String transactionStatus = request.getParameter("vnp_TransactionStatus");
            if ("00".equals(transactionStatus)) {
                return 1; // SUCCESS
            } else {
                return 0; // FAILED
            }
        } else {
            return -1; // INVALID SIGNATURE
        }
    }
}
