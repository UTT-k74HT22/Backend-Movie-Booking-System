# 🐛 REMAINING BUGS - TODO LIST

> **Status:** 3/12 bugs fixed ✅  
> **Branch:** `develop`  
> **Last Updated:** 2025-11-11

---

## ✅ COMPLETED (3/12)

| # | Bug | Branch | Status | PR |
|---|-----|--------|--------|----|
| 1 | Showtime Validation | `bugfix/showtime-validation` | ✅ MERGED | #20 |
| 2 | BookingSeat Denormalized Fields | `bugfix/booking-seat-denormalized-fields` | ✅ MERGED | #21 |
| 3 | Booking Expiration | `bugfix/booking-expiration` | ✅ PUSHED | Pending PR |

**Time Spent:** ~4-5 hours

---

## 🔴 CRITICAL PRIORITY (4 bugs - 8-12 hours)

### BUG #4: Payment Idempotency ⚠️ HIGH PRIORITY
**Estimate:** 2-3 hours  
**Severity:** 🔴 CRITICAL  
**Impact:** Duplicate payments, financial loss

**Problem:**
- User refresh payment page → Duplicate payment
- Network retry → Multiple charges
- No idempotency key checking

**Solution:**
```java
// Add idempotency_key to payment_transactions table
@Column(name = "idempotency_key", unique = true)
private String idempotencyKey;

// Check before processing
if (paymentRepo.existsByIdempotencyKey(key)) {
    return existingPayment;
}
```

**Files to modify:**
- `PaymentTransaction.java` - Add field
- `PaymentTransactionRepository.java` - Add query
- `PaymentService.java` - Check idempotency
- `VNPayService.java` - Generate/validate key

**Testing:**
- Refresh payment page 3 times → Only 1 payment
- Network timeout → Retry safe

---

### BUG #5: Payment IPN/Webhook ⚠️ HIGH PRIORITY
**Estimate:** 3-4 hours  
**Severity:** 🔴 CRITICAL  
**Impact:** Lost payments if user closes browser

**Problem:**
- User pays → Closes browser before redirect
- Payment successful but booking not confirmed
- Money taken but no ticket

**Solution:**
```java
@PostMapping("/payment/webhook/vnpay")
public ResponseEntity<String> handleVNPayWebhook(@RequestParam Map<String, String> params) {
    // 1. Validate signature
    if (!vnpayService.validateSignature(params)) {
        return ResponseEntity.status(400).body("Invalid signature");
    }
    
    // 2. Update booking status
    String txnRef = params.get("vnp_TxnRef");
    String responseCode = params.get("vnp_ResponseCode");
    
    if ("00".equals(responseCode)) {
        bookingService.confirmPayment(txnRef);
    }
    
    return ResponseEntity.ok("OK");
}
```

**Files to modify:**
- `PaymentController.java` - Add webhook endpoint
- `VNPayService.java` - Add signature validation
- `BookingService.java` - Add confirmPayment method

**Config:**
```yaml
# application.yml
vnpay:
  webhook-url: ${BACKEND_URL}/api/payment/webhook/vnpay
  secret-key: ${VNPAY_SECRET_KEY}
```

**Testing:**
- Pay → Close browser immediately
- Check webhook called
- Booking status = CONFIRMED

---

### BUG #6: VNPay Mock → Real Integration 🔥 URGENT
**Estimate:** 2-3 days  
**Severity:** 🔴 CRITICAL (Security Risk)  
**Impact:** Production cannot use mock payment

**Problem:**
```java
// Current: FAKE payment always success
public String createPaymentUrl(...) {
    return "http://localhost:3000/payment/success"; // MOCK!
}
```

**Solution:**
1. Register VNPay sandbox account
2. Get real credentials (TMN_CODE, HASH_SECRET)
3. Implement real payment URL generation
4. Implement return URL handler
5. Add security validation

**Real Implementation:**
```java
public String createPaymentUrl(PaymentRequest request) {
    String vnp_TmnCode = vnpayConfig.getTmnCode();
    String vnp_HashSecret = vnpayConfig.getHashSecret();
    String vnp_Url = vnpayConfig.getPaymentUrl();
    
    Map<String, String> params = new TreeMap<>();
    params.put("vnp_Version", "2.1.0");
    params.put("vnp_Command", "pay");
    params.put("vnp_TmnCode", vnp_TmnCode);
    params.put("vnp_Amount", String.valueOf(amount * 100));
    params.put("vnp_TxnRef", txnRef);
    params.put("vnp_OrderInfo", orderInfo);
    params.put("vnp_ReturnUrl", returnUrl);
    // ... other params
    
    String signData = buildSignData(params);
    String secureHash = hmacSHA512(vnp_HashSecret, signData);
    params.put("vnp_SecureHash", secureHash);
    
    return vnp_Url + "?" + buildQueryString(params);
}
```

**Steps:**
1. Get VNPay sandbox credentials
2. Update `application.yml` with real config
3. Implement signature generation/validation
4. Test with VNPay sandbox
5. Document for production deployment

---

### BUG #7: Cleanup Completed Showtimes 🧹
**Estimate:** 1-2 hours  
**Severity:** 🔴 CRITICAL  
**Impact:** Redis seats locked forever

**Problem:**
```java
// Showtime ended but seats still "booked" in Redis
// End time: 2025-11-10 22:00
// Current: 2025-11-11 10:00
// Redis still has: showtime:1:seats:booked = [1,2,3]
```

**Solution:**
```java
@Scheduled(cron = "0 0 * * * *")  // Every hour
public void cleanupCompletedShowtimes() {
    LocalDateTime now = LocalDateTime.now();
    
    // Find showtimes that ended > 1 hour ago
    List<Showtime> completedShowtimes = showtimeRepository
        .findCompletedShowtimes(now.minusHours(1));
    
    for (Showtime showtime : completedShowtimes) {
        // Clear Redis data
        redisTemplate.delete("showtime:" + showtime.getId() + ":*");
        
        // Optional: Update showtime status
        showtime.setStatus(ShowtimeStatus.COMPLETED);
        showtimeRepository.save(showtime);
        
        log.info("Cleaned up showtime {}", showtime.getId());
    }
}
```

**Files to modify:**
- `ShowtimeRepository.java` - Add query
- `ShowtimeCleanupService.java` - New cron job
- `ShowtimeStatus.java` - Add COMPLETED enum

**Query:**
```java
@Query("""
    SELECT s FROM Showtime s 
    WHERE s.status = 'ACTIVE'
    AND FUNCTION('TIMESTAMP', s.showDate, s.endTime) < :cutoffTime
""")
List<Showtime> findCompletedShowtimes(@Param("cutoffTime") LocalDateTime cutoffTime);
```

---

## 🟡 MAJOR PRIORITY (3 bugs - 4-6 hours)

### BUG #8: Pagination Missing
**Estimate:** 1-2 hours  
**Severity:** 🟡 MAJOR  
**Impact:** Performance issue, can't load large datasets

**Current:**
```java
@Override
public PageResponse<?> getAlls(int pageNumber, int pageSize) {
    throw new UnsupportedOperationException("Pagination not yet implemented");
}
```

**Solution:**
```java
@Override
public PageResponse<BookingResponse> getAlls(int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, 
        Sort.by(Sort.Direction.DESC, "bookingDate"));
    
    Page<Booking> bookingPage = bookingRepository.findAll(pageable);
    
    List<BookingResponse> content = bookingPage.getContent()
        .stream()
        .map(BookingMapper::toResponse)
        .toList();
    
    return PageResponse.<BookingResponse>builder()
        .page(pageNumber)
        .size(pageSize)
        .totalPages(bookingPage.getTotalPages())
        .totalElements(bookingPage.getTotalElements())
        .data(content)
        .build();
}
```

**Bonus: Add filters:**
- By user ID
- By status
- By date range
- By showtime

---

### BUG #9: Email Templates + QR Code
**Estimate:** 2-3 hours  
**Severity:** 🟡 MAJOR  
**Impact:** Poor UX, manual ticket verification

**Current:**
```java
// Plain text email
emailService.sendSimpleEmail(
    user.getEmail(),
    "Booking Confirmation",
    "Your booking ID: " + booking.getId()
);
```

**Solution:**
```html
<!-- email-template.html -->
<!DOCTYPE html>
<html>
<head>
    <style>
        .ticket { border: 2px solid #000; padding: 20px; }
        .qr-code { text-align: center; }
    </style>
</head>
<body>
    <div class="ticket">
        <h1>🎬 Movie Ticket</h1>
        <p>Booking Code: <strong>{{bookingCode}}</strong></p>
        <p>Movie: {{movieName}}</p>
        <p>Showtime: {{showtime}}</p>
        <p>Seats: {{seats}}</p>
        
        <div class="qr-code">
            <img src="cid:qrcode" alt="QR Code"/>
        </div>
    </div>
</body>
</html>
```

**QR Code Generation:**
```java
// Add dependency: com.google.zxing
public byte[] generateQRCode(String bookingCode) {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(
        bookingCode, 
        BarcodeFormat.QR_CODE, 
        300, 300
    );
    
    ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
    return pngOutputStream.toByteArray();
}
```

---

### BUG #10: Redis Connection Pool
**Estimate:** 30 minutes  
**Severity:** 🟡 MAJOR  
**Impact:** Performance degradation under load

**Current:**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # Missing pool config!
```

**Solution:**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      database: 0
      timeout: 3000
      lettuce:
        pool:
          max-active: 20    # Max connections
          max-idle: 10      # Max idle connections
          min-idle: 5       # Min idle connections
          max-wait: 1000ms  # Max wait time
        shutdown-timeout: 100ms
```

---

## 🟢 MINOR PRIORITY (2 bugs - 2-3 hours)

### BUG #11: Structured Logging
**Estimate:** 1 hour  
**Severity:** 🟢 MINOR  
**Impact:** Hard to debug in production

**Current:**
```java
log.info("Booking created: {}", booking.getId());
```

**Solution:**
```java
// Use structured logging with MDC
MDC.put("userId", userId.toString());
MDC.put("bookingId", booking.getId().toString());
MDC.put("action", "CREATE_BOOKING");

log.info("Booking created successfully", 
    kv("userId", userId),
    kv("bookingId", booking.getId()),
    kv("totalPrice", booking.getTotalPrice()),
    kv("seatCount", booking.getBookingSeats().size())
);

MDC.clear();
```

**Config Logback:**
```xml
<pattern>%d{ISO8601} [%thread] %-5level %logger{36} [userId=%X{userId}] [bookingId=%X{bookingId}] - %msg%n</pattern>
```

---

### BUG #12: i18n Error Messages
**Estimate:** 1-2 hours  
**Severity:** 🟢 MINOR  
**Impact:** No internationalization support

**Current:**
```java
throw new BadRequestException("Seat already booked");
```

**Solution:**
```properties
# messages_en.properties
error.seat.already.booked=Seat {0} is already booked
error.showtime.past=Cannot book for past showtime

# messages_vi.properties
error.seat.already.booked=Ghế {0} đã được đặt
error.showtime.past=Không thể đặt vé cho suất chiếu đã qua
```

```java
@Autowired
private MessageSource messageSource;

throw new BadRequestException(
    messageSource.getMessage(
        "error.seat.already.booked",
        new Object[]{seatId},
        LocaleContextHolder.getLocale()
    )
);
```

---

## 📊 SUMMARY

| Priority | Count | Est. Time | Status |
|----------|-------|-----------|--------|
| ✅ **Done** | 3 | 4-5h | COMPLETED |
| 🔴 **Critical** | 4 | 8-12h | TODO |
| 🟡 **Major** | 3 | 4-6h | TODO |
| 🟢 **Minor** | 2 | 2-3h | TODO |
| **TOTAL** | **12** | **18-26h** | **25% Done** |

---

## 🎯 RECOMMENDED ORDER

### Week 1: Critical Security & Payment (Priority 1)
1. ✅ ~~BUG #1: Showtime Validation~~ - DONE
2. ✅ ~~BUG #2: BookingSeat Fields~~ - DONE
3. ✅ ~~BUG #3: Booking Expiration~~ - DONE
4. **BUG #4: Payment Idempotency** ⚠️ START HERE
5. **BUG #5: Payment IPN/Webhook** ⚠️ URGENT
6. **BUG #6: VNPay Real Integration** 🔥 CRITICAL

### Week 2: Cleanup & Performance (Priority 2)
7. **BUG #7: Cleanup Showtimes**
8. **BUG #8: Pagination**
9. **BUG #10: Redis Pool**

### Week 3: UX & Polish (Priority 3)
10. **BUG #9: Email + QR Code**
11. **BUG #11: Structured Logging**
12. **BUG #12: i18n Messages**

---

## 🚀 NEXT TASK: BUG #4 - Payment Idempotency

**Ready to start?** Đọc task details ở trên và bắt đầu implement!

**Branch naming:**
```bash
git checkout -b bugfix/payment-idempotency
```

**Estimated completion:** 2-3 hours  
**Priority:** 🔴 CRITICAL - START IMMEDIATELY

---

**Questions?** Ask before starting! 🤝
