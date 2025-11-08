package com.trainning.movie_booking_system.dto.response.Voucher;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for voucher validation result
 * Contains validation status and calculated discount
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherValidationResult {

    private Boolean isValid;
    
    private String message;
    
    private String voucherCode;
    
    private String voucherName;
    
    // Original booking amount before discount
    private BigDecimal originalAmount;
    
    // Discount amount calculated based on voucher rules
    private BigDecimal discountAmount;
    
    // Final amount after applying discount
    private BigDecimal finalAmount;
    
    // Remaining usage for this user
    private Integer remainingUsage;
    
    // Voucher expiry info
    private String validUntil;
}
