# ✅ TASK CHECKLIST - BUG #3

Branch: `bugfix/booking-expiration` ✅ (đã tạo từ develop)

---

## 🎯 YOUR TASKS

### 1️⃣ Update Booking Entity
- [ ] Mở `Booking.java`
- [ ] Thêm field: `private LocalDateTime expiresAt;`
- [ ] Thêm `@Column(name = "expires_at", nullable = false)`
- [ ] Thêm index: `@Index(name = "idx_booking_expires", columnList = "status, expires_at")`

### 2️⃣ Update Repository Query
- [ ] Mở `BookingRepository.java`
- [ ] Tìm method `findAllExpiredBookings()`
- [ ] Sửa query: `b.bookingDate` → `b.expiresAt`
- [ ] Sửa param: `expiryTime` → `now`

### 3️⃣ Update Service
- [ ] Mở `BookingServiceImpl.java`
- [ ] Tìm method `createBookingTransaction()`
- [ ] Thêm `.expiresAt(LocalDateTime.now().plusMinutes(15))`

### 4️⃣ Update Cron Job
- [ ] Mở `BookingExpireService.java`
- [ ] Sửa call: `LocalDateTime.now().minusMinutes(15)` → `LocalDateTime.now()`

### 5️⃣ Test
```bash
# Compile
mvn compile -DskipTests

# Run app
mvn spring-boot:run

# Check database
DESC bookings;
# Should see: expires_at column

# Test create booking
POST /api/bookings

# Check expires_at set correctly
SELECT booking_date, expires_at FROM bookings ORDER BY id DESC LIMIT 1;
```
- [ ] Compile thành công
- [ ] App start OK
- [ ] Database có column `expires_at`
- [ ] Booking mới có expiresAt = bookingDate + 15 min

### 6️⃣ Commit & Push
```bash
git status
git add .
git commit -m "fix: Add expiresAt field to Booking..."
git push origin bugfix/booking-expiration
```

---

## 📖 CHI TIẾT

Đọc file: **TASK_BUG_3_BOOKING_EXPIRATION.md**

---

## ⏱️ Estimated: 1.5-2 giờ

Good luck! 🚀
