# ✅ TASK CHECKLIST - BUG #1

Branch: `bugfix/showtime-validation` ✅ (đã tạo từ develop)

---

## 🎯 YOUR TASKS

### 1️⃣ Code Implementation
- [ ] Mở file `BookingServiceImpl.java`
- [ ] Tìm method `create(BookingRequest request)` (line 67)
- [ ] Sau dòng 82 (get showtime), thêm: `validateShowtime(showtime);`
- [ ] Kéo xuống cuối class, thêm method `validateShowtime()` (xem TASK_BUG_1)
- [ ] Thêm imports: `LocalDateTime`, `DateTimeFormatter`

### 2️⃣ Write Tests
- [ ] Mở/tạo file `BookingServiceTest.java`
- [ ] Thêm test: `testBookingPastShowtime()`
- [ ] Thêm test: `testBookingWithinCutoffTime()`
- [ ] Thêm test: `testBookingFutureShowtime()`

### 3️⃣ Run Tests
```bash
mvn test
```
- [ ] All tests pass ✅

### 4️⃣ Manual Test với Postman
- [ ] Test 1: Book past showtime → 400 Bad Request
- [ ] Test 2: Book within cutoff → 400 Bad Request  
- [ ] Test 3: Book future showtime → 201 Created

### 5️⃣ Commit & Push
```bash
git status
git add .
git commit -m "fix: Add showtime validation..."
git push origin bugfix/showtime-validation
```

### 6️⃣ Create Pull Request
- [ ] Title: `fix: Add showtime validation...`
- [ ] Description: Dùng template trong TASK_BUG_1
- [ ] Base: `develop`
- [ ] Assign reviewers

---

## 📖 CHI TIẾT

Đọc file: **TASK_BUG_1_SHOWTIME_VALIDATION.md**

---

## ⏱️ Estimated: 30 phút

Good luck! 🚀
