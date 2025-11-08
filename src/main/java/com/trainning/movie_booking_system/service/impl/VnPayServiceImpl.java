package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.config.VnPayProperties;
import com.trainning.movie_booking_system.helper.VnPay.VnPayHelper;
import com.trainning.movie_booking_system.service.VnPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
        Map<String, String> vnp = new HashMap<>();
        vnp.put("vnp_Version", "2.1.0");
        vnp.put("vnp_Command", "pay");
        vnp.put("vnp_TmnCode", props.getTmnCode());
        vnp.put("vnp_Amount", String.valueOf(amountVnd * 100)); // x100
        vnp.put("vnp_CurrCode", "VND");
        vnp.put("vnp_TxnRef", txnRef);
        vnp.put("vnp_OrderInfo", orderInfo);
        vnp.put("vnp_OrderType", "other"); // hoặc "billpayment" tuỳ bạn
        vnp.put("vnp_Locale", "vn"); // "vn" hoặc "en"
        vnp.put("vnp_ReturnUrl", props.getReturnUrl());
        vnp.put("vnp_IpAddr", clientIp);
        String now = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now());
        vnp.put("vnp_CreateDate", now);
        // Optional: vnp_ExpireDate (yyyymmddHHMMss)
        // Optional: vnp_BankCode (NCB...), vnp_Bill_... for billing

        // Thêm IPN khi cần (nhiều cấu hình dùng IPN cấu hình sẵn trong portal; bạn vẫn có thể gửi kèm)
        vnp.put("vnp_Url", props.getPayUrl()); // chỉ để tiện debug, không gửi vào VNPay params

        // Ký
        Map<String, String> toSign = new HashMap<>(vnp);
        toSign.remove("vnp_Url");
        String hash = VnPayHelper.secureHash(props.getHashSecret(), toSign);
        toSign.put("vnp_SecureHash", hash);

        String query = VnPayHelper.buildQuery(toSign);
        return props.getPayUrl() + "?" + query;
    }
}
