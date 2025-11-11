# 🎯 TASK: Fix BUG #3 - Booking Expiration

> **Branch:** `bugfix/booking-expiration` (✅ Đã tạo từ `develop`)  
> **Assignee:** Bạn  
> **Estimate:** 2-3 giờ  
> **Priority:** 🔴 CRITICAL  
> **Status:** 🟡 IN PROGRESS

---

## 📝 PROBLEM DESCRIPTION

**Hiện tại:**
- Cron job `BookingExpireService` đã tồn tại ✅
- Nhưng query trong repository sử dụng `bookingDate` ❌
- Booking entity **THIẾU** field `expiresAt` ❌
- Không có cơ chế set expiration time khi tạo booking ❌

**Current Issues:**

1. **Repository query SAI:**
```java
// ❌ Dùng bookingDate thay vì expiresAt
findAllExpiredBookings(..., LocalDateTime.now().minusMinutes(15))
```

2. **Booking entity thiếu field:**
```java
@Entity
public class Booking {
    private LocalDateTime bookingDate;  // ✅ Có
    // ❌ THIẾU: private LocalDateTime expiresAt;
}
```

3. **Service không set expiration:**
```java
// BookingServiceImpl.createBookingTransaction()
var booking = Booking.builder()
    .status(PENDING_PAYMENT)
    .build();
// ❌ THIẾU: .expiresAt(LocalDateTime.now().plusMinutes(15))
```

---

## 🎯 ACCEPTANCE CRITERIA

### ✅ Scenario 1: Booking có expiresAt field
```java
Booking booking = bookingRepository.findById(1);
assertNotNull(booking.getExpiresAt());
assertEquals(
    booking.getBookingDate().plusMinutes(15),
    booking.getExpiresAt()
);
```

### ✅ Scenario 2: Expired bookings được tìm đúng
```sql
-- Booking created at: 10:00
-- expiresAt: 10:15
-- Current time: 10:20

SELECT * FROM bookings 
WHERE status = 'PENDING_PAYMENT' 
AND expires_at < NOW();

-- Should return this booking
```

### ✅ Scenario 3: Cron job expire bookings
```
Given: Booking created 20 minutes ago
When: Cron job runs
Then: 
  - Booking status = EXPIRED
  - Redis seats released
  - Log shows "Booking ID X expired"
```

### ✅ Scenario 4: Valid booking not expired
```
Given: Booking created 5 minutes ago (expiresAt in 10 minutes)
When: Cron job runs
Then: Booking status still PENDING_PAYMENT
```

---

## 📂 FILES TO MODIFY

### 1. Entity Layer
**File:** `src/main/java/com/trainning/movie_booking_system/entity/Booking.java`
- Add `expiresAt` field
- Add index on `expires_at` column

### 2. Repository Layer
**File:** `src/main/java/com/trainning/movie_booking_system/repository/BookingRepository.java`
- Fix query: use `expiresAt` instead of `bookingDate`

### 3. Service Layer
**File:** `src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java`
- Set `expiresAt` when creating booking

### 4. Cron Job (Already exists ✅)
**File:** `src/main/java/com/trainning/movie_booking_system/helper/cron/BookingExpireService.java`
- Code is good, just need entity + repo fix

---

## 💡 IMPLEMENTATION GUIDE

### Step 1: Add expiresAt to Booking Entity

**File:** `Booking.java`

**TODO: Thêm field sau `bookingDate`:**
```java
@Column(name = "booking_date", nullable = false)
private LocalDateTime bookingDate;

// TODO: ADD THIS
@Column(name = "expires_at", nullable = false)
private LocalDateTime expiresAt;
```

**TODO: Thêm index trong `@Table`:**
```java
@Table(
    name = "bookings",
    indexes = {
        @Index(name = "idx_booking_account", columnList = "account_id"),
        @Index(name = "idx_booking_showtime", columnList = "showtime_id"),
        @Index(name = "idx_booking_status", columnList = "status"),
        // TODO: ADD THIS
        @Index(name = "idx_booking_expires", columnList = "status, expires_at")
    }
)
```

**Giải thích index:**
- `status, expires_at` → Optimize query tìm expired bookings
- Cron job query: `WHERE status = 'PENDING_PAYMENT' AND expires_at < NOW()`

---

### Step 2: Update Repository Query

**File:** `BookingRepository.java`

**Current (SAI):**
```java
@Query("SELECT b FROM Booking b WHERE b.status = :status AND b.bookingDate < :expiryTime")
List<Booking> findAllExpiredBookings(
    @Param("status") BookingStatus status,
    @Param("expiryTime") LocalDateTime expiryTime
);
```

**TODO: Sửa thành:**
```java
@Query("SELECT b FROM Booking b WHERE b.status = :status AND b.expiresAt < :now")
List<Booking> findAllExpiredBookings(
    @Param("status") BookingStatus status,
    @Param("now") LocalDateTime now
);
```

**Giải thích:**
- `expiresAt < :now` → Lấy bookings đã hết hạn
- Parameter name: `expiryTime` → `now` (clear hơn)

---

### Step 3: Set expiresAt When Creating Booking

**File:** `BookingServiceImpl.java`

**Tìm method `createBookingTransaction()` → vị trí tạo Booking:**

**Current:**
```java
var booking = Booking.builder()
        .account(account)
        .showtime(showtime)
        .status(BookingStatus.PENDING_PAYMENT)
        .build();
```

**TODO: Thêm expiresAt:**
```java
var booking = Booking.builder()
        .account(account)
        .showtime(showtime)
        .status(BookingStatus.PENDING_PAYMENT)
        // TODO: ADD THIS - Expire after 15 minutes
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .build();
```

**Giải thích:**
- User có 15 phút để thanh toán
- Sau 15 phút → Cron job tự động expire
- Business rule: Same as showtime cutoff time

---

### Step 4: Update Cron Job Call (If needed)

**File:** `BookingExpireService.java`

**Current code đã đúng:**
```java
List<Booking> expiredBookings = bookingRepository.findAllExpiredBookings(
    BookingStatus.PENDING_PAYMENT,
    LocalDateTime.now().minusMinutes(15)  // ← Sẽ sửa trong repo
);
```

**TODO: Đơn giản hóa call:**
```java
List<Booking> expiredBookings = bookingRepository.findAllExpiredBookings(
    BookingStatus.PENDING_PAYMENT,
    LocalDateTime.now()  // ← So sánh với now, không cần minusMinutes
);
```

**Giải thích:**
- Query mới: `expiresAt < NOW()`
- Không cần tính toán `now - 15 minutes` nữa
- Logic clear hơn

---

## 🧪 TESTING TASKS

### Task 1: Database Schema Test
```bash
# Start app
mvn spring-boot:run

# Check database
mysql -u root -p movie_booking
DESC bookings;

# Expected:
# expires_at DATETIME NOT NULL

SHOW INDEX FROM bookings;
# Expected:
# idx_booking_expires (status, expires_at)
```

### Task 2: Unit Test - Set ExpiresAt

**File:** `BookingServiceTest.java`

```java
@Test
void testBookingHasExpiresAt() {
    // Arrange
    BookingRequest request = createValidRequest();
    
    // Act
    LocalDateTime beforeCreate = LocalDateTime.now();
    BookingResponse response = bookingService.create(request);
    LocalDateTime afterCreate = LocalDateTime.now();
    
    // Assert
    Booking booking = bookingRepository.findById(response.getId()).get();
    assertNotNull(booking.getExpiresAt());
    
    // expiresAt should be bookingDate + 15 minutes
    assertEquals(
        booking.getBookingDate().plusMinutes(15),
        booking.getExpiresAt()
    );
    
    // expiresAt should be ~15 minutes from now
    assertTrue(booking.getExpiresAt().isAfter(beforeCreate.plusMinutes(14)));
    assertTrue(booking.getExpiresAt().isBefore(afterCreate.plusMinutes(16)));
}
```

### Task 3: Integration Test - Cron Job

```java
@Test
void testExpireBookingCronJob() {
    // Arrange: Create booking 20 minutes ago
    Booking booking = createBookingWithCustomTime(
        LocalDateTime.now().minusMinutes(20),  // bookingDate
        LocalDateTime.now().minusMinutes(5)    // expiresAt (expired)
    );
    booking.setStatus(BookingStatus.PENDING_PAYMENT);
    bookingRepository.save(booking);
    
    // Act: Run cron job manually
    bookingExpireService.expireBookings();
    
    // Assert
    Booking expiredBooking = bookingRepository.findById(booking.getId()).get();
    assertEquals(BookingStatus.EXPIRED, expiredBooking.getStatus());
}

@Test
void testValidBookingNotExpired() {
    // Arrange: Create booking 5 minutes ago (still valid)
    Booking booking = createBookingWithCustomTime(
        LocalDateTime.now().minusMinutes(5),   // bookingDate
        LocalDateTime.now().plusMinutes(10)    // expiresAt (not expired)
    );
    booking.setStatus(BookingStatus.PENDING_PAYMENT);
    bookingRepository.save(booking);
    
    // Act
    bookingExpireService.expireBookings();
    
    // Assert
    Booking validBooking = bookingRepository.findById(booking.getId()).get();
    assertEquals(BookingStatus.PENDING_PAYMENT, validBooking.getStatus());
}
```

### Task 4: Manual Test

**Test Scenario 1: Create booking**
```bash
POST http://localhost:8080/api/bookings
{
  "showtimeId": 1,
  "seatIds": [1, 2]
}

# Check database
SELECT id, booking_date, expires_at, status FROM bookings ORDER BY id DESC LIMIT 1;

# Expected:
# booking_date: 2025-11-11 10:00:00
# expires_at:   2025-11-11 10:15:00  (+ 15 minutes)
# status:       PENDING_PAYMENT
```

**Test Scenario 2: Wait 16 minutes → Check cron**
```bash
# Wait for cron to run (every 5 minutes)
# Or trigger manually in code

# Check database
SELECT id, status FROM bookings WHERE id = [your_booking_id];

# Expected:
# status: EXPIRED
```

---

## 🔍 VALIDATION CHECKLIST

- [ ] Booking entity has `expiresAt` field
- [ ] Database has `expires_at` column
- [ ] Index created on `(status, expires_at)`
- [ ] Repository query uses `expiresAt`
- [ ] Service sets `expiresAt` when creating booking
- [ ] Cron job finds expired bookings correctly
- [ ] Expired bookings status changed to EXPIRED
- [ ] Redis seats released
- [ ] Valid bookings not affected
- [ ] All tests pass

---

## 🚀 COMMIT TEMPLATE

```bash
git add src/main/java/com/trainning/movie_booking_system/entity/Booking.java
git add src/main/java/com/trainning/movie_booking_system/repository/BookingRepository.java
git add src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java
git add src/main/java/com/trainning/movie_booking_system/helper/cron/BookingExpireService.java

git commit -m "fix: Add expiresAt field to Booking for proper expiration handling

WHAT CHANGED:
- Added expiresAt field to Booking entity
- Added index on (status, expires_at) for query optimization
- Updated repository query to use expiresAt instead of bookingDate
- Service sets expiresAt = bookingDate + 15 minutes
- Simplified cron job call to use LocalDateTime.now()

WHY:
- Previous logic used bookingDate which is incorrect
- No way to track when booking should expire
- Cron job couldn't find expired bookings accurately
- Business rule: 15 minutes to complete payment

HOW IT WORKS:
- When booking created: expiresAt = now + 15 minutes
- Cron runs every 5 minutes
- Query: WHERE status = PENDING_PAYMENT AND expiresAt < NOW()
- If found → status = EXPIRED, release Redis holds

PERFORMANCE:
- Composite index (status, expires_at) for fast queries
- Cron job efficient with indexed WHERE clause

TESTING:
- Unit tests for expiresAt field
- Integration tests for cron job
- Manual testing verified

Fixes: BUG #3 - Booking Expiration
Related: BUGS_TO_FIX.md"

git push origin bugfix/booking-expiration
```

---

## ⚠️ IMPORTANT NOTES

### Database Migration
- JPA auto-update will create `expires_at` column
- **Existing data:** Will have NULL in `expires_at`
- **Solution:** Update existing PENDING_PAYMENT bookings:

```sql
-- Option 1: Set expired (safest)
UPDATE bookings 
SET status = 'EXPIRED' 
WHERE status = 'PENDING_PAYMENT' 
AND expires_at IS NULL;

-- Option 2: Set expiresAt (if want to keep)
UPDATE bookings 
SET expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE)
WHERE expires_at IS NULL;
```

### Cron Schedule
- Current: Every 5 minutes (`0 */5 * * * *`)
- Good balance: không spam DB, expire kịp thời
- Consider: Adjust to 1 minute if needed

### Business Logic
- **15 minutes timeout:** Same as showtime booking cutoff
- **EXPIRED status:** Giữ record để audit, không xóa
- **Redis release:** Cho phép user khác book seats

---

## 💡 TIPS

### Tip 1: Test Cron Manually
```java
@Autowired
private BookingExpireService bookingExpireService;

// In test or controller
bookingExpireService.expireBookings();
```

### Tip 2: Enable Scheduling Logs
```yaml
# application.yml
logging:
  level:
    com.trainning.movie_booking_system.helper.cron: DEBUG
```

### Tip 3: Verify Index
```sql
EXPLAIN SELECT * FROM bookings 
WHERE status = 'PENDING_PAYMENT' 
AND expires_at < NOW();

-- Should see: Using index idx_booking_expires
```

---

## ⏱️ TIME ESTIMATE

| Task | Time |
|------|------|
| Add expiresAt field | 10 min |
| Update repository query | 5 min |
| Update service | 10 min |
| Update cron job | 5 min |
| Write tests | 30 min |
| Manual testing | 20 min |
| Fix existing data | 10 min |
| **Total** | **1h 30min** |

---

**Ready to implement?** Start with Step 1! 🚀
