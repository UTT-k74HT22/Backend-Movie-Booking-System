# 🎯 TASK: Fix BUG #1 - Showtime Validation

> **Branch:** `bugfix/showtime-validation` (✅ Đã tạo từ `develop`)  
> **Assignee:** Bạn  
> **Estimate:** 30 phút  
> **Priority:** 🔴 CRITICAL  
> **Status:** 🟡 IN PROGRESS

---

## 📝 PROBLEM DESCRIPTION

**Hiện tại:**
- User có thể đặt vé cho suất chiếu đã qua
- User có thể đặt vé ngay trước giờ chiếu (1-2 phút)
- Không có validation về thời gian showtime

**Ví dụ lỗi:**
```
Hôm nay: 11/11/2025 20:00
Showtime: 10/11/2025 20:00 (ĐÃ QUA!)
➡️ API vẫn cho phép book ❌
```

**Business Rules bị vi phạm:**
1. ❌ Không được đặt vé cho suất chiếu đã bắt đầu
2. ❌ Phải đóng booking 15 phút trước giờ chiếu

---

## 🎯 ACCEPTANCE CRITERIA

### ✅ Scenario 1: Reject past showtime
```
GIVEN: Showtime đã qua (10/11/2025 20:00)
WHEN: User tạo booking
THEN: 
  - Trả về 400 Bad Request
  - Message: "Cannot book for past showtime. Showtime was at 2025-11-10 20:00"
```

### ✅ Scenario 2: Reject within cutoff time
```
GIVEN: Showtime bắt đầu trong 10 phút (11/11/2025 20:10, hiện tại 20:00)
WHEN: User tạo booking
THEN:
  - Trả về 400 Bad Request
  - Message: "Booking closes 15 minutes before showtime. Cutoff time was 2025-11-10 19:55"
```

### ✅ Scenario 3: Allow future showtime
```
GIVEN: Showtime ngày mai (12/11/2025 20:00)
WHEN: User tạo booking
THEN:
  - Booking được tạo thành công
  - Trả về 201 Created
```

---

## 📂 FILES TO MODIFY

### 1. Service Layer (MAIN TASK - BẠN LÀM)
**File:** `src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java`

**Location:** Method `create(BookingRequest request)`

**Current Code:**
```java
@Override
public BookingDTO create(BookingRequest request) {
    // 1. Get showtime
    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
    
    // ❌ MISSING: Validate showtime here!
    
    // 2. Get seats
    List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
    // ... rest of code
}
```

**Your Task:** Thêm validation sau khi lấy showtime

---

## 💡 IMPLEMENTATION GUIDE

### Step 1: Thêm method helper (cuối class `BookingServiceImpl`)

```java
/**
 * Validate showtime chưa bắt đầu và còn thời gian đặt vé
 * 
 * @param showtime Showtime cần validate
 * @throws BadRequestException nếu không hợp lệ
 */
private void validateShowtime(Showtime showtime) {
    LocalDateTime now = LocalDateTime.now();
    
    // TODO: Tính toán thời điểm bắt đầu showtime
    // Hint: Combine showtime.getShowDate() + showtime.getStartTime()
    LocalDateTime showtimeStart = LocalDateTime.of(
        showtime.getShowDate(),
        showtime.getStartTime()
    );
    
    // TODO: Rule 1 - Kiểm tra showtime đã qua chưa
    // Hint: if (showtimeStart.isBefore(now)) { throw ... }
    
    
    // TODO: Rule 2 - Kiểm tra còn 15 phút trước giờ chiếu
    // Hint: LocalDateTime cutoffTime = showtimeStart.minusMinutes(15);
    //       if (now.isAfter(cutoffTime)) { throw ... }
    
    
    log.debug("Showtime validation passed for showtime ID: {}", showtime.getId());
}
```

### Step 2: Gọi validation trong `create()` method

```java
@Override
public BookingDTO create(BookingRequest request) {
    // 1. Get showtime
    Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
    
    // TODO: Thêm dòng này
    // validateShowtime(showtime);
    
    // 2. Get seats
    List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
    // ... rest of code
}
```

### Step 3: Thêm imports cần thiết

```java
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
```

---

## 🧪 TESTING TASKS

### Task 1: Write Unit Tests
**File:** `src/test/java/com/trainning/movie_booking_system/service/BookingServiceTest.java`

**Test Case 1: Past showtime**
```java
@Test
@DisplayName("Should throw BadRequestException when booking past showtime")
void testBookingPastShowtime() {
    // TODO: Arrange
    // - Tạo Showtime với showDate = yesterday
    // - Mock showtimeRepository.findById()
    
    // TODO: Act & Assert
    // - assertThrows(BadRequestException.class, ...)
}
```

**Test Case 2: Within cutoff time**
```java
@Test
@DisplayName("Should throw BadRequestException when booking within cutoff time")
void testBookingWithinCutoffTime() {
    // TODO: Arrange
    // - Tạo Showtime bắt đầu sau 10 phút
    // - Mock repository
    
    // TODO: Act & Assert
    // - assertThrows(BadRequestException.class, ...)
}
```

**Test Case 3: Future showtime (should pass)**
```java
@Test
@DisplayName("Should allow booking for future showtime")
void testBookingFutureShowtime() {
    // TODO: Arrange
    // - Tạo Showtime ngày mai
    // - Mock all dependencies
    
    // TODO: Act
    // - BookingDTO result = bookingService.create(request);
    
    // TODO: Assert
    // - assertNotNull(result);
}
```

### Task 2: Run Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=BookingServiceTest

# Expected: ✅ All tests pass
```

---

## 📋 MANUAL TESTING CHECKLIST

### Setup Test Data
Tạo showtime test trong database:

```sql
-- Showtime đã qua (để test reject)
INSERT INTO showtimes (show_date, start_time, end_time, screen_id, movie_id, price)
VALUES ('2025-11-10', '20:00:00', '22:00:00', 1, 1, 150000);

-- Showtime sắp bắt đầu (để test cutoff)
INSERT INTO showtimes (show_date, start_time, end_time, screen_id, movie_id, price)
VALUES ('2025-11-11', '20:10:00', '22:10:00', 1, 1, 150000);

-- Showtime tương lai (để test success)
INSERT INTO showtimes (show_date, start_time, end_time, screen_id, movie_id, price)
VALUES ('2025-11-15', '20:00:00', '22:00:00', 1, 1, 150000);
```

### Test với Postman

**Test 1: Book past showtime (Should FAIL)**
```http
POST http://localhost:8080/api/bookings
Authorization: Bearer {your_token}
Content-Type: application/json

{
  "showtimeId": 1,  // Showtime 10/11/2025
  "seatIds": [1, 2]
}

✅ Expected Response:
{
  "success": false,
  "status": 400,
  "message": "Cannot book for past showtime. Showtime was at 2025-11-10 20:00"
}
```

**Test 2: Book within cutoff (Should FAIL)**
```http
POST http://localhost:8080/api/bookings
{
  "showtimeId": 2,  // Showtime 11/11/2025 20:10 (còn 10 phút)
  "seatIds": [3, 4]
}

✅ Expected Response:
{
  "success": false,
  "status": 400,
  "message": "Booking closes 15 minutes before showtime. Cutoff time was 2025-11-11 19:55"
}
```

**Test 3: Book future showtime (Should SUCCESS)**
```http
POST http://localhost:8080/api/bookings
{
  "showtimeId": 3,  // Showtime 15/11/2025
  "seatIds": [5, 6]
}

✅ Expected Response:
{
  "success": true,
  "status": 201,
  "data": {
    "id": 123,
    "bookingCode": "BK20251111123456",
    "status": "PENDING_PAYMENT",
    "totalPrice": 300000,
    ...
  }
}
```

---

## ✅ DEFINITION OF DONE

- [ ] Code implementation hoàn thành
- [ ] `validateShowtime()` method được thêm vào `BookingServiceImpl`
- [ ] Method được gọi trong `create()` 
- [ ] Imports được thêm đầy đủ
- [ ] Unit tests được viết (3 test cases)
- [ ] All tests pass (`mvn test`)
- [ ] Manual testing với Postman (3 scenarios)
- [ ] Code được format đúng chuẩn
- [ ] No compilation errors
- [ ] Logging được thêm đúng chỗ

---

## 🚀 SUBMIT YOUR WORK

### Step 1: Check your changes
```bash
git status
git diff
```

### Step 2: Commit
```bash
git add src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java
git add src/test/java/com/trainning/movie_booking_system/service/BookingServiceTest.java

git commit -m "fix: Add showtime validation to prevent booking past showtimes

WHAT CHANGED:
- Added validateShowtime() method in BookingServiceImpl
- Validates showtime hasn't started
- Validates booking within cutoff time (15 minutes before)

WHY:
- Users could book tickets for past showtimes
- No validation for showtime start time
- Business rule: booking closes 15 minutes before showtime

TESTING:
- Added 3 unit tests (past showtime, cutoff time, future showtime)
- All existing tests still pass
- Manual testing with Postman completed

Fixes: BUG #1 - Showtime Validation
Related: BUGS_TO_FIX.md"
```

### Step 3: Push
```bash
git push origin bugfix/showtime-validation
```

### Step 4: Create Pull Request
**Title:** `fix: Add showtime validation to prevent booking past showtimes`

**Description:**
```markdown
## 🎯 What
Add validation to prevent booking for past showtimes and within cutoff time.

## ❌ Problem
- Users could book tickets for showtimes that already started
- No business rule enforcement for booking cutoff time

## ✅ Solution
- Added `validateShowtime()` method in `BookingServiceImpl`
- Check showtime hasn't started (past validation)
- Check booking within 15-minute cutoff window

## 🧪 Testing
- ✅ Unit tests: 3 new tests added, all pass
- ✅ Integration tests: All existing tests pass  
- ✅ Manual testing: Tested with Postman (3 scenarios)

## 📸 Screenshots
(Attach Postman test results)

## 📝 Related
- Fixes: BUG #1 in `BUGS_TO_FIX.md`
- Base branch: `develop`
- Reviewers: @hoangdinhdung05
```

---

## 💡 HINTS & TIPS

### Hint 1: Calculate showtime start
```java
// Combine LocalDate + LocalTime = LocalDateTime
LocalDateTime showtimeStart = LocalDateTime.of(
    showtime.getShowDate(),    // LocalDate: 2025-11-10
    showtime.getStartTime()     // LocalTime: 20:00
);
// Result: 2025-11-10T20:00:00
```

### Hint 2: Compare dates
```java
LocalDateTime now = LocalDateTime.now();

if (showtimeStart.isBefore(now)) {
    // Showtime đã qua
}

if (now.isAfter(cutoffTime)) {
    // Vượt quá thời gian cutoff
}
```

### Hint 3: Format error message
```java
String formattedTime = showtimeStart.format(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
);
// Result: "2025-11-10 20:00"
```

### Hint 4: Exception handling
```java
throw new BadRequestException("Your error message here");
```

---

## 📞 NEED HELP?

**Stuck?** Check these resources:
1. `BUGS_TO_FIX.md` - Bug details
2. `HOW_TO_FIX_BUGS.md` - Full solution (nếu cần)
3. `BookingServiceImpl.java` - Existing code
4. Java LocalDateTime docs: https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html

**Questions?** Ask me! 🤝

---

## ⏱️ TIME TRACKING

| Task | Estimated | Actual | Notes |
|------|-----------|--------|-------|
| Code implementation | 15 min | ___ | validateShowtime() method |
| Unit tests | 10 min | ___ | 3 test cases |
| Manual testing | 5 min | ___ | Postman tests |
| **Total** | **30 min** | ___ | |

---

**Ready to code?** 💪

1. Đọc kỹ requirements
2. Implement code trong `BookingServiceImpl.java`
3. Write tests
4. Run `mvn test`
5. Test với Postman
6. Commit & Push

**Good luck!** 🚀
