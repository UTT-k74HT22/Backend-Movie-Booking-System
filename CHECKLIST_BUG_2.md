# ✅ TASK CHECKLIST - BUG #2

Branch: `bugfix/booking-seat-denormalized-fields` ✅ (đã tạo từ develop)

---

## 🎯 YOUR TASKS

### 1️⃣ Create Database Migration (QUAN TRỌNG!)
- [ ] Tạo folder (nếu chưa có): `src/main/resources/db/migration/`
- [ ] Tạo file: `V2__add_booking_seat_denormalized_fields.sql`
- [ ] Copy SQL từ TASK_BUG_2 vào file
- [ ] Verify SQL syntax

### 2️⃣ Update Entity
- [ ] Mở `BookingSeat.java`
- [ ] Thêm 3 fields: `seatNumber`, `rowLabel`, `seatType`
- [ ] Thêm index annotation vào `@Table`

### 3️⃣ Update DTO
- [ ] Mở `BookingSeatDTO.java`
- [ ] Thêm 3 fields: `seatNumber`, `rowLabel`, `seatType`
- [ ] Thêm method `getSeatLabel()` (computed field)

### 4️⃣ Update Service
- [ ] Mở `BookingServiceImpl.java`
- [ ] Tìm method `createBookingTransaction()`
- [ ] Trong vòng lặp tạo BookingSeat, thêm 3 fields copy từ Seat

### 5️⃣ Run Migration & Test
```bash
# Option 1: Flyway
mvn flyway:migrate

# Option 2: Spring Boot (auto-update)
mvn spring-boot:run

# Verify database
mysql -u root -p
DESC booking_seats;
```
- [ ] Migration chạy thành công
- [ ] Columns mới xuất hiện
- [ ] Data cũ được migrate

### 6️⃣ Test API
- [ ] Start app: `mvn spring-boot:run`
- [ ] Postman: Create booking
- [ ] Response có `seatNumber`, `rowLabel`, `seatType`, `seatLabel`

### 7️⃣ Commit & Push
```bash
git status
git add .
git commit -m "fix: Add denormalized seat info..."
git push origin bugfix/booking-seat-denormalized-fields
```

---

## 📖 CHI TIẾT

Đọc file: **TASK_BUG_2_BOOKING_SEAT_FIELDS.md**

---

## ⚠️ IMPORTANT

**BẮT ĐẦU TỪ MIGRATION!** Phải tạo columns trong DB trước khi update code.

Order: Migration → Entity → DTO → Service → Test

---

## ⏱️ Estimated: 1-2 giờ

Good luck! 🚀
