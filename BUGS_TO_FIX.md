# 🐛 TỔNG HỢP CÁC BUG CẦN FIX

> **Ngày phát hiện:** November 11, 2025  
> **Trạng thái:** Chưa fix  
> **Ưu tiên:** Theo thứ tự từ CRITICAL → MAJOR → MINOR

---

## 📊 OVERVIEW

| Priority | Count | Description |
|----------|-------|-------------|
| 🔴 **CRITICAL** | 7 bugs | Must fix trước khi production |
| 🟠 **MAJOR** | 3 bugs | High impact, cần fix sớm |
| 🟡 **MINOR** | 2 bugs | Nice to have |
| **TOTAL** | **12 bugs** | |

---

## 🔴 CRITICAL BUGS (Must Fix!)

### BUG #1: Showtime Validation - Có thể book suất chiếu đã qua

**Severity:** 🔴 CRITICAL  
**Impact:** User có thể đặt vé cho suất chiếu đã chiếu hoặc đang chiếu  
**Effort:** 30 phút

**Current State:**
```java
// BookingServiceImpl.java - KHÔNG CÓ validation!
public BookingDTO create(BookingRequest request) {
    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
    
    // ❌ THIẾU: Kiểm tra showtime chưa bắt đầu!
    // User có thể book cho suất chiếu đã qua
}
```

**Fix Required:**
```java
private void validateShowtime(Showtime showtime) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime showtimeStart = LocalDateTime.of(
        showtime.getShowDate(),
        showtime.getStartTime()
    );
    
    // 1. Không được book suất chiếu đã qua
    if (showtimeStart.isBefore(now)) {
        throw new BadRequestException("Cannot book for past showtime");
    }
    
    // 2. Đóng booking 15 phút trước giờ chiếu
    LocalDateTime cutoff = showtimeStart.minusMinutes(15);
    if (now.isAfter(cutoff)) {
        throw new BadRequestException(
            "Booking closes 15 minutes before showtime"
        );
    }
}
```

**Files to Modify:**
- `BookingServiceImpl.java` - Add validation method
- Test với suất chiếu: quá khứ, hiện tại, tương lai

---

### BUG #2: BookingSeat thiếu rowLabel - Không biết ghế hàng nào

**Severity:** 🔴 CRITICAL  
**Impact:** Frontend không hiển thị được thông tin ghế đầy đủ (A1, B2, C3...)  
**Effort:** 1 giờ  
**Phát hiện bởi:** User

**Current State:**
```java
@Entity
@Table(name = "booking_seats")
public class BookingSeat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;  // Có quan hệ với Seat
    
    private BigDecimal price;
    
    // ❌ THIẾU: Không có rowLabel, seatNumber
    // Frontend phải join thêm với Seat table để lấy thông tin!
}
```

**Seat Entity (Có đầy đủ info):**
```java
public class Seat {
    private int seatNumber;       // 1, 2, 3...
    private String rowLabel;      // A, B, C...
    private SeatType seatType;    // STANDARD, VIP
    // ...
}
```

**Issue:**
- DTO trả về booking chỉ có `seatId` và `price`
- Frontend phải gọi thêm API để lấy `rowLabel` và `seatNumber`
- Performance issue: N+1 queries

**Fix Required:**

**Option 1: Thêm denormalized fields (RECOMMENDED)**
```java
@Entity
@Table(name = "booking_seats")
public class BookingSeat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Booking booking;
    
    @ManyToOne(fetch = FetchType.LAZY)
    private Seat seat;
    
    private BigDecimal price;
    
    // ✅ ADD: Denormalized fields để query nhanh
    @Column(name = "seat_number", nullable = false)
    private int seatNumber;
    
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;
}
```

**Migration:**
```sql
ALTER TABLE booking_seats 
ADD COLUMN seat_number INT NOT NULL AFTER seat_id,
ADD COLUMN row_label VARCHAR(10) NOT NULL AFTER seat_number,
ADD COLUMN seat_type VARCHAR(20) NOT NULL AFTER row_label;

-- Migrate existing data
UPDATE booking_seats bs
JOIN seats s ON bs.seat_id = s.id
SET bs.seat_number = s.seat_number,
    bs.row_label = s.row_label,
    bs.seat_type = s.seat_type;

CREATE INDEX idx_booking_seat_info ON booking_seats(row_label, seat_number);
```

**Update Service:**
```java
// BookingServiceImpl.java
private BookingSeat createBookingSeat(Booking booking, Seat seat, BigDecimal price) {
    return BookingSeat.builder()
        .booking(booking)
        .seat(seat)
        .price(price)
        // ✅ ADD: Copy seat info
        .seatNumber(seat.getSeatNumber())
        .rowLabel(seat.getRowLabel())
        .seatType(seat.getSeatType())
        .build();
}
```

**Update DTO:**
```java
public class BookingSeatDTO {
    private Long id;
    private Long seatId;
    private BigDecimal price;
    
    // ✅ ADD: Display info
    private int seatNumber;
    private String rowLabel;
    private SeatType seatType;
    private String seatLabel;  // "A1", "B2", "C3" (computed)
    
    public String getSeatLabel() {
        return rowLabel + seatNumber;
    }
}
```

**Files to Modify:**
- `BookingSeat.java` - Add fields
- Migration SQL
- `BookingServiceImpl.java` - Update create logic
- `BookingSeatDTO.java` - Add fields
- `BookingMapper.java` - Map new fields

---

### BUG #3: Booking chưa có expiresAt - Không tự động expire

**Severity:** 🔴 CRITICAL  
**Impact:** Bookings PENDING_PAYMENT không bao giờ expire, ghế bị lock vĩnh viễn  
**Effort:** 3 giờ

**Current State:**
```java
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {
    // ... existing fields
    
    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;
    
    // ❌ THIẾU: expires_at field!
    // Cron job không có cách nào query được bookings cần expire
}
```

**Fix Required:**

**1. Update Entity:**
```java
@Entity
public class Booking extends BaseEntity {
    // ... existing fields
    
    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;
    
    // ✅ ADD: Expiration timestamp
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @PrePersist
    protected void onCreate() {
        bookingDate = LocalDateTime.now();
        // Set expiration: 15 minutes from now for PENDING_PAYMENT
        if (this.status == BookingStatus.PENDING_PAYMENT) {
            this.expiresAt = LocalDateTime.now().plusMinutes(15);
        }
    }
}
```

**2. Database Migration:**
```sql
-- Add column
ALTER TABLE bookings
ADD COLUMN expires_at TIMESTAMP NULL AFTER booking_date;

-- Create index for cron query
CREATE INDEX idx_booking_expires_at ON bookings(expires_at);

-- Set expires_at for existing PENDING_PAYMENT bookings (15 min from booking_date)
UPDATE bookings
SET expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE)
WHERE status = 'PENDING_PAYMENT' AND expires_at IS NULL;
```

**3. Update Repository:**
```java
public interface BookingRepository extends JpaRepository<Booking, Long> {
    // ✅ ADD: Query for cron job
    List<Booking> findByStatusAndExpiresAtBefore(
        BookingStatus status, 
        LocalDateTime now
    );
}
```

**4. Update Cron Job:**
```java
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingExpireService {
    
    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    public void expireBookings() {
        LocalDateTime now = LocalDateTime.now();
        
        // ✅ Query bookings expired
        List<Booking> expiredBookings = bookingRepository
            .findByStatusAndExpiresAtBefore(
                BookingStatus.PENDING_PAYMENT,
                now
            );
        
        if (expiredBookings.isEmpty()) {
            log.debug("No bookings to expire");
            return;
        }
        
        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);
            
            // Release seat holds (if any still in Redis)
            List<Long> seatIds = booking.getBookingSeats().stream()
                .map(bs -> bs.getSeat().getId())
                .toList();
            
            seatDomainService.releaseHolds(
                booking.getShowtime().getId(),
                seatIds
            );
        }
        
        bookingRepository.saveAll(expiredBookings);
        log.info("Expired {} bookings", expiredBookings.size());
    }
}
```

**Files to Modify:**
- `Booking.java` - Add expiresAt field
- Migration SQL
- `BookingRepository.java` - Add query method
- `BookingExpireService.java` - Implement cron logic
- `BookingServiceImpl.java` - Set expiresAt when creating

---

### BUG #4: Payment Idempotency - Duplicate processing

**Severity:** 🔴 CRITICAL  
**Impact:** VNPay callback có thể được gọi nhiều lần (user refresh, IPN retry) → duplicate confirmation  
**Effort:** 3 giờ

**Scenario:**
```
10:00:00 - User thanh toán thành công
10:00:05 - VNPay Return URL callback → Booking CONFIRMED ✅
10:00:06 - User refresh page → Callback lại → Process again! ❌
10:00:10 - VNPay IPN retry #1 → Callback lại → Process again! ❌
10:00:20 - VNPay IPN retry #2 → Callback lại → Process again! ❌
```

**Current State:**
```java
// PaymentController.java - KHÔNG CÓ idempotency check!
@PostMapping("/vnpay/callback")
public ResponseEntity<?> callback(@RequestParam Map<String, String> params) {
    // ❌ Process ngay mà không check đã process chưa!
    Booking booking = bookingRepository.findById(bookingId)...;
    booking.setStatus(BookingStatus.CONFIRMED);
    // Có thể gọi nhiều lần!
}
```

**Fix Required:**

**1. Add Redis Lock:**
```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public PaymentResponse handleVNPayReturn(Map<String, String> params) {
    String txnRef = params.get("vnp_TxnRef");
    
    // 1. Acquire Redis lock
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(
        "payment:lock:" + txnRef, 
        "locked", 
        30, 
        TimeUnit.SECONDS
    );
    
    if (!locked) {
        throw new ConflictException("Payment is being processed");
    }
    
    try {
        // 2. Load transaction with pessimistic lock
        PaymentTransaction txn = repository
            .findByTransactionIdForUpdate(txnRef);
        
        // 3. IDEMPOTENCY CHECK
        if (txn.getStatus() != PaymentStatus.PENDING) {
            log.warn("Duplicate callback for txn: {}, status: {}", 
                txnRef, txn.getStatus());
            return buildCachedResponse(txn);
        }
        
        // 4. Process payment (only if PENDING)
        return processPayment(txn, params);
        
    } finally {
        // 5. Release lock
        redisTemplate.delete("payment:lock:" + txnRef);
    }
}
```

**2. Add Pessimistic Lock:**
```java
public interface PaymentTransactionRepository {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.transactionId = :txnRef")
    Optional<PaymentTransaction> findByTransactionIdForUpdate(
        @Param("txnRef") String txnRef
    );
}
```

**Files to Modify:**
- `PaymentServiceImpl.java` - Add idempotency logic
- `PaymentTransactionRepository.java` - Add pessimistic lock
- Add integration test

---

### BUG #5: Payment IPN Webhook - Không có endpoint

**Severity:** 🔴 CRITICAL  
**Impact:** Nếu user đóng browser, payment success không được capture  
**Effort:** 4 giờ

**Why Critical:**
- Return URL callback phụ thuộc browser (user có thể đóng)
- IPN là server-to-server notification (reliable)
- VNPay auto-retry lên đến 10 lần

**Current State:**
```java
// PaymentController.java - CHỈ CÓ Return URL!
@GetMapping("/vnpay/callback")
public ResponseEntity<?> vnpayCallback(...) {
    // User redirect callback
}

// ❌ THIẾU: IPN webhook endpoint!
```

**Fix Required:**
```java
@PostMapping("/vnpay/ipn")
public ResponseEntity<Map<String, String>> vnpayIPN(
    @RequestParam Map<String, String> params,
    HttpServletRequest request
) {
    log.info("VNPay IPN received from IP: {}", request.getRemoteAddr());
    
    // 1. Verify signature
    if (!vnPayService.verifySignature(params)) {
        log.error("Invalid IPN signature: {}", params);
        return ResponseEntity.ok(Map.of(
            "RspCode", "97",
            "Message", "Invalid signature"
        ));
    }
    
    // 2. Reuse same payment processing logic
    try {
        PaymentResponse response = paymentService.handleVNPayReturn(params);
        
        // 3. Return VNPay format (IMPORTANT!)
        return ResponseEntity.ok(Map.of(
            "RspCode", "00",
            "Message", "Confirm Success"
        ));
    } catch (Exception e) {
        log.error("IPN processing failed", e);
        return ResponseEntity.ok(Map.of(
            "RspCode", "99",
            "Message", "Error"
        ));
    }
}
```

**VNPay IPN Response Codes:**
- `00` - Success (VNPay stops retry)
- `99` - Error (VNPay retries up to 10 times)

**Files to Modify:**
- `PaymentController.java` - Add IPN endpoint
- `application.yml` - Configure IPN URL
- Add PaymentWebhookLog entity for audit

---

### BUG #6: Payment Gateway - MOCK Implementation

**Severity:** 🔴 CRITICAL  
**Impact:** Không có payment thực sự, attacker có thể fake payment  
**Effort:** 2-3 ngày

**Current State:**
```java
// PaymentServiceImpl.java - MOCK!
public String createPaymentUrl(BookingDTO booking) {
    return "http://mock-payment-gateway.com/checkout?bookingId=" 
        + booking.getId();
}
```

**Fix Required:**
See `docs/05-PAYMENT-FLOW.md` for full VNPay integration guide.

---

### BUG #7: Booking sau showtime kết thúc chưa xóa ghế

**Severity:** 🔴 CRITICAL  
**Impact:** Sau khi xem phim xong, ghế vẫn bị giữ, user khác không đặt được cho suất chiếu mới  
**Effort:** 2 giờ  
**Phát hiện bởi:** User

**Scenario:**
```
Suất chiếu: 2024-11-11 20:00 - 22:30 (Spider-Man, 150 phút)
User A đặt ghế A1, A2 → CONFIRMED
─────────────────────────────────────────
22:30 - Phim kết thúc
22:35 - Ghế A1, A2 vẫn thuộc booking cũ
─────────────────────────────────────────
Suất chiếu mới: 2024-11-12 10:00 (cùng screen)
User B muốn đặt A1 → KHÔNG ĐẶT ĐƯỢC!
❌ Ghế vẫn linked với booking cũ
```

**Current State:**
```java
// BookingExpireService.java - CHỈ expire PENDING_PAYMENT!
@Scheduled(cron = "0 */5 * * * *")
public void expireBookings() {
    List<Booking> expiredBookings = bookingRepository
        .findByStatusAndExpiresAtBefore(
            BookingStatus.PENDING_PAYMENT,  // ❌ CHỈ PENDING_PAYMENT
            LocalDateTime.now()
        );
    // Không xử lý CONFIRMED bookings sau showtime!
}
```

**Root Cause:**
1. Booking entity không validate ghế theo showtime
2. Seat availability check không filter theo showtime
3. Ghế từ booking cũ (đã xem xong) vẫn "occupied"

**Fix Required:**

**Option 1: Soft-delete booking_seats sau showtime (RECOMMENDED)**
```java
@Scheduled(cron = "0 0 3 * * *") // 3 AM daily
public void cleanupCompletedShowtimes() {
    LocalDateTime now = LocalDateTime.now();
    
    // 1. Find all showtimes ended > 24 hours ago
    List<Showtime> completedShowtimes = showtimeRepository
        .findCompletedShowtimesBefore(now.minusHours(24));
    
    for (Showtime showtime : completedShowtimes) {
        // 2. Get all confirmed bookings for this showtime
        List<Booking> bookings = bookingRepository
            .findByShowtimeAndStatus(showtime, BookingStatus.CONFIRMED);
        
        // 3. Update status to USED (completed)
        for (Booking booking : bookings) {
            booking.setStatus(BookingStatus.USED);
        }
        
        bookingRepository.saveAll(bookings);
    }
    
    log.info("Cleaned up {} completed showtimes", completedShowtimes.size());
}
```

**Option 2: Filter seat availability by showtime**
```java
// SeatDomainService.java
public List<Seat> getAvailableSeats(Long showtimeId) {
    Showtime showtime = showtimeRepository.findById(showtimeId)...;
    
    // Get all seats for this screen
    List<Seat> allSeats = seatRepository.findByScreenId(showtime.getScreen().getId());
    
    // Filter out booked seats FOR THIS SHOWTIME ONLY
    List<Long> bookedSeatIds = bookingRepository
        .findBookedSeatIdsByShowtime(showtimeId);
    
    return allSeats.stream()
        .filter(seat -> !bookedSeatIds.contains(seat.getId()))
        .toList();
}
```

**Add BookingStatus.USED:**
```java
public enum BookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    USED  // ✅ ADD: Đã check-in và xem phim xong
}
```

**Files to Modify:**
- `BookingExpireService.java` - Add cleanup cron
- `ShowtimeRepository.java` - Add findCompletedShowtimesBefore()
- `BookingStatus.java` - Add USED enum
- `SeatDomainService.java` - Filter by showtime

---

## 🟠 MAJOR BUGS (High Priority)

### BUG #8: Pagination chưa implement

**Severity:** 🟠 MAJOR  
**Impact:** API trả về ALL records → performance issue, memory issue  
**Effort:** 2 giờ

**Current State:**
```java
// BookingController.java
@GetMapping
public ResponseEntity<List<BookingDTO>> getAll() {
    throw new UnsupportedOperationException("Not implemented yet");
}
```

**Fix Required:**
```java
@GetMapping
public ResponseEntity<Page<BookingDTO>> getBookings(
    @RequestParam(required = false) BookingStatus status,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate startDate,
    @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate endDate,
    @PageableDefault(size = 20, sort = "bookingDate", direction = DESC) Pageable pageable
) {
    Page<Booking> bookings = bookingService.findBookings(
        status, startDate, endDate, pageable
    );
    return ResponseEntity.ok(bookings.map(mapper::toDTO));
}
```

**Files to Modify:**
- `BookingController.java` - Add pagination
- `BookingService.java` - Implement with Specification API
- Add filters: status, dateRange, userId

---

### BUG #9: Email templates quá đơn giản

**Severity:** 🟠 MAJOR  
**Impact:** User experience kém, thiếu QR code cho check-in  
**Effort:** 1 ngày

**Current State:**
Plain text emails without booking details.

**Fix Required:**
- HTML email templates với Thymeleaf
- QR code generation
- Booking details (movie, showtime, seats, price)

---

### BUG #10: Redis connection pool chưa configure

**Severity:** 🟠 MAJOR  
**Impact:** Performance issue khi high traffic  
**Effort:** 30 phút

**Fix Required:**
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
          max-wait: 3000ms
```

---

## 🟡 MINOR BUGS (Nice to Have)

### BUG #11: Logging chưa structured

**Severity:** 🟡 MINOR  
**Impact:** Khó debug, không có correlation ID  
**Effort:** 4 giờ

**Fix:** Use Logback MDC, add correlation ID filter

---

### BUG #12: Error messages hardcoded

**Severity:** 🟡 MINOR  
**Impact:** Không hỗ trợ i18n  
**Effort:** 2 giờ

**Fix:** Externalize to messages.properties

---

## 📋 IMPLEMENTATION ORDER (Recommended)

### Week 1: Critical Fixes (Days 1-3)
```
Day 1 (4h):
  ✅ BUG #1: Showtime validation (30 min)
  ✅ BUG #3: Booking expiresAt (3h)
  ✅ Test both

Day 2 (6h):
  ✅ BUG #2: BookingSeat rowLabel (1h)
  ✅ BUG #4: Payment idempotency (3h)
  ✅ BUG #5: Payment IPN webhook (2h)

Day 3 (4h):
  ✅ BUG #7: Cleanup completed showtimes (2h)
  ✅ Integration testing (2h)
```

### Week 2: Payment Integration
```
Day 4-5 (2 days):
  ✅ BUG #6: VNPay integration (2-3 days)
  ✅ Testing with VNPay sandbox

Day 6-7:
  ✅ BUG #8: Pagination (2h)
  ✅ BUG #9: Email templates (1 day)
  ✅ BUG #10: Redis pool config (30 min)
```

### Week 3: Polish
```
  ✅ BUG #11: Logging (4h)
  ✅ BUG #12: i18n (2h)
  ✅ Final testing
```

---

## 🧪 TESTING CHECKLIST

Sau mỗi bug fix, test:
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing với Postman
- [ ] Edge cases (concurrency, timeout, errors)
- [ ] Performance testing (nếu cần)

---

## 📚 RELATED DOCS

- [Booking Flow](docs/04-BOOKING-FLOW.md) - Context cho bug #1, #2, #3, #7
- [Payment Flow](docs/05-PAYMENT-FLOW.md) - Context cho bug #4, #5, #6
- [TODO List](docs/06-TODO.md) - Implementation details

---

**🎯 Bắt đầu với:** BUG #1 (Showtime validation) - Easiest, 30 phút!
