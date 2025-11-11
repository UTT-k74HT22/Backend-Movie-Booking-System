# 🔄 Các Flow nghiệp vụ - Movie Booking System

> Chi tiết về các luồng xử lý nghiệp vụ và hướng dẫn test

## 📋 Danh sách Flows

1. [Registration & Activation Flow](#1-registration--activation-flow)
2. [Login & Authentication Flow](#2-login--authentication-flow)
3. [Forgot & Reset Password Flow](#3-forgot--reset-password-flow)
4. [Movie Search & Browse Flow](#4-movie-search--browse-flow)
5. [Seat Hold & Booking Flow](#5-seat-hold--booking-flow-critical)
6. [Payment Flow](#6-payment-flow-mock)
7. [Booking Expiration Flow](#7-booking-expiration-flow-auto)

---

## 1. 📝 Registration & Activation Flow

### 📖 Mô tả
User đăng ký tài khoản mới và kích hoạt qua email OTP.

### 🔄 Flow chi tiết

```
┌─────────┐                ┌─────────┐              ┌──────────┐         ┌───────┐
│  User   │                │ Backend │              │  MySQL   │         │ Email │
└────┬────┘                └────┬────┘              └────┬─────┘         └───┬───┘
     │                          │                        │                    │
     │ 1. POST /api/auth/register                       │                    │
     │ {username, email, password, ...}                 │                    │
     ├─────────────────────────>│                        │                    │
     │                          │                        │                    │
     │                          │ 2. Validate input      │                    │
     │                          │   - Username unique?   │                    │
     │                          │   - Email unique?      │                    │
     │                          ├───────────────────────>│                    │
     │                          │                        │                    │
     │                          │ 3. Hash password (BCrypt)                  │
     │                          │                        │                    │
     │                          │ 4. Create account      │                    │
     │                          │   status=ACTIVE        │                    │
     │                          │   email_verified=false │                    │
     │                          ├───────────────────────>│                    │
     │                          │                        │                    │
     │                          │ 5. Create user profile │                    │
     │                          ├───────────────────────>│                    │
     │                          │                        │                    │
     │                          │ 6. Generate OTP (6 digits)                 │
     │                          │    Store in Redis (TTL: 5 min)             │
     │                          │                        │                    │
     │                          │ 7. Send OTP email      │                    │
     │                          ├───────────────────────────────────────────>│
     │                          │                        │                    │
     │   200 OK                 │                        │                    │
     │   "Check your email"     │                        │                    │
     │<─────────────────────────┤                        │                    │
     │                          │                        │                    │
     │ 8. Check email           │                        │                    │
     │<────────────────────────────────────────────────────────────────────────┤
     │                          │                        │                    │
     │ 9. POST /api/auth/activate                       │                    │
     │ {email, otp}             │                        │                    │
     ├─────────────────────────>│                        │                    │
     │                          │                        │                    │
     │                          │ 10. Verify OTP from Redis                  │
     │                          │     (match & not expired?)                 │
     │                          │                        │                    │
     │                          │ 11. Update account     │                    │
     │                          │     email_verified=true│                    │
     │                          ├───────────────────────>│                    │
     │                          │                        │                    │
     │                          │ 12. Delete OTP from Redis                  │
     │                          │                        │                    │
     │   200 OK                 │                        │                    │
     │   "Account activated"    │                        │                    │
     │<─────────────────────────┤                        │                    │
```

### ✅ Validation Rules

**RegisterRequest:**
- `username`: 4-30 ký tự, unique
- `email`: Valid email format, unique
- `password`: Min 8 ký tự
- `firstName`, `lastName`: Max 50 ký tự
- `phoneNumber`: Format số điện thoại Việt Nam

**OTP:**
- 6 chữ số
- TTL: 5 phút
- Rate limit: Max 5 lần gửi/ngày, 60s cooldown giữa các lần gửi

### 🧪 Test Cases

#### Happy Path
```bash
# 1. Register
POST http://localhost:8080/api/auth/register
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Test@1234",
  "firstName": "Test",
  "lastName": "User",
  "phoneNumber": "0912345678"
}
Expected: 200 OK

# 2. Check email → Get OTP (e.g., "123456")

# 3. Activate
POST http://localhost:8080/api/auth/activate
{
  "email": "test@example.com",
  "otp": "123456"
}
Expected: 200 OK, email_verified=true
```

#### Error Cases
```bash
# 1. Duplicate username
POST /api/auth/register với username đã tồn tại
Expected: 400 Bad Request, "Username already exists"

# 2. Invalid OTP
POST /api/auth/activate với OTP sai
Expected: 400 Bad Request, "Invalid or expired OTP"

# 3. Expired OTP
Đợi > 5 phút → POST /api/auth/activate
Expected: 400 Bad Request, "Invalid or expired OTP"

# 4. Rate limit OTP
Gửi OTP > 5 lần trong 24h
Expected: 429 Too Many Requests (nếu implement)
```

### ⚠️ Issues & Improvements

**Current Issues:**
- ❌ Không có endpoint resend OTP
- ❌ Không có rate limiting cho registration (chỉ có cho OTP)
- ⚠️ OTP gửi qua email plain text (nên dùng HTML template đẹp hơn)

**Improvements:**
- ✅ Add POST /api/otp/resend
- ✅ Add rate limiting cho registration (10 req/min per IP)
- ✅ Improve email template

---

## 2. 🔐 Login & Authentication Flow

### 📖 Mô tả
User đăng nhập và nhận JWT tokens (access + refresh).

### 🔄 Flow chi tiết

```
┌─────────┐              ┌─────────┐            ┌──────────┐        ┌───────┐
│  User   │              │ Backend │            │  MySQL   │        │ Redis │
└────┬────┘              └────┬────┘            └────┬─────┘        └───┬───┘
     │                        │                      │                   │
     │ 1. POST /api/auth/login                      │                   │
     │ {username, password}   │                      │                   │
     ├───────────────────────>│                      │                   │
     │                        │                      │                   │
     │                        │ 2. Load account from DB                  │
     │                        ├─────────────────────>│                   │
     │                        │                      │                   │
     │                        │ 3. Validate:         │                   │
     │                        │    - Password (BCrypt compare)           │
     │                        │    - email_verified=true?                │
     │                        │    - status=ACTIVE?  │                   │
     │                        │                      │                   │
     │                        │ 4. Generate JWT access token             │
     │                        │    (TTL: 30 min)     │                   │
     │                        │                      │                   │
     │                        │ 5. Generate refresh token                │
     │                        │    (TTL: 1 day)      │                   │
     │                        │                      │                   │
     │                        │ 6. Store refresh token in Redis          │
     │                        │    Key: "auth:refreshToken:{username}"   │
     │                        ├──────────────────────────────────────────>│
     │                        │                      │                   │
     │  200 OK                │                      │                   │
     │  {accessToken, refreshToken}                 │                   │
     │<───────────────────────┤                      │                   │
     │                        │                      │                   │
     │ 7. Subsequent requests │                      │                   │
     │    Header: Authorization: Bearer {accessToken}                    │
     ├───────────────────────>│                      │                   │
     │                        │                      │                   │
     │                        │ 8. JwtFilter validates token             │
     │                        │    - Signature valid?│                   │
     │                        │    - Not expired?    │                   │
     │                        │    - User exists?    │                   │
     │                        │                      │                   │
     │  Response              │                      │                   │
     │<───────────────────────┤                      │                   │
```

### 🔑 JWT Token Structure

**Access Token (30 min):**
```json
{
  "sub": "username",
  "roles": ["ROLE_USER"],
  "iat": 1699999999,
  "exp": 1700001799
}
```

**Refresh Token (1 day):**
```json
{
  "sub": "username",
  "iat": 1699999999,
  "exp": 1700086399
}
```

### 🔄 Token Refresh Flow

```
┌─────────┐              ┌─────────┐            ┌───────┐
│  User   │              │ Backend │            │ Redis │
└────┬────┘              └────┬────┘            └───┬───┘
     │                        │                      │
     │ Access token expired!  │                      │
     │ POST /api/auth/refresh-token                 │
     │ {refreshToken}         │                      │
     ├───────────────────────>│                      │
     │                        │                      │
     │                        │ 1. Validate refresh token                │
     │                        │    - Signature valid?│                   │
     │                        │    - Not expired?    │                   │
     │                        │                      │                   │
     │                        │ 2. Get stored token from Redis           │
     │                        │    Key: "auth:refreshToken:{username}"   │
     │                        ├──────────────────────>│                   │
     │                        │                      │                   │
     │                        │ 3. Compare tokens    │                   │
     │                        │    (match?)          │                   │
     │                        │                      │                   │
     │                        │ 4. Generate new access token             │
     │                        │    (TTL: 30 min)     │                   │
     │                        │                      │                   │
     │  200 OK                │                      │                   │
     │  {accessToken, refreshToken}                 │                   │
     │<───────────────────────┤                      │                   │
```

### 🧪 Test Cases

#### Happy Path
```bash
# 1. Login
POST http://localhost:8080/api/auth/login
{
  "username": "testuser",
  "password": "Test@1234"
}
Expected: 200 OK
Response: {
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}

# 2. Test authenticated endpoint
GET http://localhost:8080/api/auth/me
Header: Authorization: Bearer {accessToken}
Expected: 200 OK

# 3. Refresh token (sau 30 phút)
POST http://localhost:8080/api/auth/refresh-token
{
  "refreshToken": "eyJhbGc..."
}
Expected: 200 OK, new accessToken
```

#### Error Cases
```bash
# 1. Wrong password
POST /api/auth/login với password sai
Expected: 400 Bad Request, "Invalid username or password"

# 2. Email not verified
POST /api/auth/login với account chưa activate
Expected: 400 Bad Request, "Email is not verified"

# 3. Invalid access token
GET /api/auth/me với token sai/expired
Expected: 401 Unauthorized

# 4. Invalid refresh token
POST /api/auth/refresh-token với token sai
Expected: 400 Bad Request, "Invalid refresh token"
```

### 🔐 Security Considerations

**✅ Implemented:**
- BCrypt password hashing
- JWT signature verification
- Refresh token stored in Redis (can be revoked)
- Stateless sessions

**⚠️ Missing:**
- ❌ Token blacklisting khi logout (chỉ delete refresh token)
- ❌ Access token vẫn valid cho đến khi hết hạn
- ❌ Không có IP whitelist/device tracking

---

## 3. 🔑 Forgot & Reset Password Flow

### 📖 Mô tả
User quên mật khẩu và đặt lại qua OTP email.

### 🔄 Flow chi tiết

```
┌─────────┐              ┌─────────┐            ┌──────────┐        ┌───────┐
│  User   │              │ Backend │            │  Redis   │        │ Email │
└────┬────┘              └────┬────┘            └────┬─────┘        └───┬───┘
     │                        │                      │                   │
     │ 1. POST /api/auth/forgot-password            │                   │
     │ {email}                │                      │                   │
     ├───────────────────────>│                      │                   │
     │                        │                      │                   │
     │                        │ 2. Check email exists (MySQL)            │
     │                        │                      │                   │
     │                        │ 3. Generate OTP (6 digits)               │
     │                        │    Store in Redis    │                   │
     │                        │    Key: "otp:FORGOT_PASSWORD:{email}"    │
     │                        │    TTL: 5 min        │                   │
     │                        ├─────────────────────>│                   │
     │                        │                      │                   │
     │                        │ 4. Send OTP email    │                   │
     │                        ├──────────────────────────────────────────>│
     │                        │                      │                   │
     │  200 OK                │                      │                   │
     │  "Check your email"    │                      │                   │
     │<───────────────────────┤                      │                   │
     │                        │                      │                   │
     │ 5. Check email         │                      │                   │
     │<───────────────────────────────────────────────────────────────────┤
     │                        │                      │                   │
     │ 6. POST /api/auth/reset-password             │                   │
     │ {email, otp, newPassword}                    │                   │
     ├───────────────────────>│                      │                   │
     │                        │                      │                   │
     │                        │ 7. Verify OTP        │                   │
     │                        ├─────────────────────>│                   │
     │                        │                      │                   │
     │                        │ 8. Hash new password (BCrypt)            │
     │                        │                      │                   │
     │                        │ 9. Update account password (MySQL)       │
     │                        │                      │                   │
     │                        │ 10. Delete OTP       │                   │
     │                        ├─────────────────────>│                   │
     │                        │                      │                   │
     │  200 OK                │                      │                   │
     │  "Password reset successfully"               │                   │
     │<───────────────────────┤                      │                   │
```

### 🧪 Test Cases

```bash
# 1. Request reset
POST http://localhost:8080/api/auth/forgot-password
{
  "email": "test@example.com"
}
Expected: 200 OK

# 2. Reset with OTP
POST http://localhost:8080/api/auth/reset-password
{
  "email": "test@example.com",
  "otp": "123456",
  "newPassword": "NewPass@123"
}
Expected: 200 OK

# 3. Login with new password
POST /api/auth/login
{
  "username": "testuser",
  "password": "NewPass@123"
}
Expected: 200 OK
```

---

## 4. 🎬 Movie Search & Browse Flow

### 📖 Mô tả
User xem danh sách phim, search với filters, và xem chi tiết.

### 🔄 Flow chi tiết

```
┌─────────┐              ┌─────────┐            ┌──────────┐
│  User   │              │ Backend │            │  MySQL   │
└────┬────┘              └────┬────┘            └────┬─────┘
     │                        │                      │
     │ 1. GET /api/movies?page=0&size=12           │
     ├───────────────────────>│                      │
     │                        │                      │
     │                        │ 2. SELECT * FROM movies                  │
     │                        │    WHERE status != 'ENDED'               │
     │                        │    ORDER BY release_date DESC            │
     │                        │    LIMIT 12 OFFSET 0 │                   │
     │                        ├─────────────────────>│                   │
     │                        │                      │                   │
     │  200 OK                │                      │                   │
     │  {content: [...], totalPages, totalElements} │                   │
     │<───────────────────────┤                      │                   │
     │                        │                      │
     │ 3. GET /api/movies/search?genre=ACTION&rating=7.5                │
     ├───────────────────────>│                      │
     │                        │                      │
     │                        │ 4. Build dynamic query (Specification)   │
     │                        │    WHERE genre LIKE '%ACTION%'           │
     │                        │    AND rating >= 7.5 │                   │
     │                        ├─────────────────────>│                   │
     │                        │                      │
     │  200 OK (filtered)     │                      │
     │<───────────────────────┤                      │
     │                        │                      │
     │ 5. GET /api/movies/{id}│                      │
     ├───────────────────────>│                      │
     │                        │                      │
     │                        │ 6. SELECT * FROM movies WHERE id = ?     │
     │                        ├─────────────────────>│                   │
     │                        │                      │
     │  200 OK (movie detail) │                      │
     │<───────────────────────┤                      │
```

### 🔍 Search Filters

**Available Filters:**
- `keyword`: Tìm trong title hoặc description
- `genres`: Set of genres (ACTION, DRAMA, COMEDY, ...)
- `language`: vi, en, ...
- `status`: COMING_SOON, NOW_SHOWING, ENDED
- `ratingMin`, `ratingMax`: Rating range (0-10)
- `durationMin`, `durationMax`: Duration range (phút)
- `releaseFrom`, `releaseTo`: Release date range
- `sortBy`: title, releaseDate, rating, duration
- `direction`: ASC, DESC

### 🧪 Test Cases

```bash
# 1. Get all movies (pagination)
GET http://localhost:8080/api/movies?pageNumber=0&pageSize=12
Expected: 200 OK, paginated response

# 2. Search action movies
GET http://localhost:8080/api/movies/search?genres=ACTION&sortBy=rating&direction=DESC
Expected: 200 OK, action movies sorted by rating

# 3. Get movie detail
GET http://localhost:8080/api/movies/1
Expected: 200 OK, movie object

# 4. Search với nhiều filters
GET /api/movies/search?keyword=spider&ratingMin=7.0&language=en&releaseFrom=2023-01-01
Expected: 200 OK, filtered results
```

---

## 5. 💺 Seat Hold & Booking Flow (CRITICAL)

### 📖 Mô tả
**Flow quan trọng nhất:** User giữ chỗ ghế tạm thời, tạo booking, và thanh toán.

### 🔄 Flow chi tiết (4 bước)

#### **STEP 1: Hold Seats (Giữ ghế tạm thời)**

```
┌─────────┐              ┌─────────┐            ┌──────────┐         ┌───────┐
│  User   │              │ Backend │            │  MySQL   │         │ Redis │
└────┬────┘              └────┬────┘            └────┬─────┘         └───┬───┘
     │                        │                      │                    │
     │ 1. Chọn ghế trên UI    │                      │                    │
     │    (showtimeId=10, seatIds=[1,2,3])          │                    │
     │                        │                      │                    │
     │ 2. POST /api/seats/hold│                      │                    │
     │ {showtimeId:10, seatIds:[1,2,3], ttlSec:120} │                    │
     ├───────────────────────>│                      │                    │
     │                        │                      │                    │
     │                        │ 3. Validate showtime exists              │
     │                        ├─────────────────────>│                    │
     │                        │                      │                    │
     │                        │ 4. Validate seats exist & belong to screen│
     │                        ├─────────────────────>│                    │
     │                        │                      │                    │
     │                        │ 5. Loop qua từng ghế:│                    │
     │                        │    Key = "hold:10:1" │                    │
     │                        │    SETNX key=userId, TTL=120s             │
     │                        ├──────────────────────────────────────────>│
     │                        │                      │                    │
     │                        │    Nếu ghế đã hold:  │                    │
     │                        │    - Check owner = current user?         │
     │                        │      → Yes: Refresh TTL                  │
     │                        │      → No: ConflictException             │
     │                        │                      │                    │
     │                        │ 6. All seats held successfully            │
     │                        │                      │                    │
     │  200 OK                │                      │                    │
     │  "Seats held for 120s" │                      │                    │
     │<───────────────────────┤                      │                    │
     │                        │                      │                    │
     │ User có 120 giây để tạo booking!             │                    │
```

**Redis Keys:**
```
Key: "hold:{showtimeId}:{seatId}"
Value: {userId}
TTL: 120 seconds
```

#### **STEP 2: Create Booking**

```
┌─────────┐              ┌─────────┐            ┌──────────┐         ┌───────┐
│  User   │              │ Backend │            │  MySQL   │         │ Redis │
└────┬────┘              └────┬────┘            └────┬─────┘         └───┬───┘
     │                        │                      │                    │
     │ 7. POST /api/bookings  │                      │                    │
     │ {showtimeId:10, seatIds:[1,2,3]}             │                    │
     ├───────────────────────>│                      │                    │
     │                        │                      │                    │
     │                        │ 8. PRE-CHECK: Verify seats held by user  │
     │                        │    assertHeldByUser(showtimeId, seatIds, userId)
     │                        ├──────────────────────────────────────────>│
     │                        │    Check Redis: hold:10:1 == userId?     │
     │                        │    If NO → ConflictException             │
     │                        │                      │                    │
     │                        │ 9. Get seat infos (VIP/STANDARD)         │
     │                        ├─────────────────────>│                    │
     │                        │                      │                    │
     │                        │ 10. ACQUIRE DISTRIBUTED LOCKS (sorted order)
     │                        │     Sort seatIds: [1,2,3]                │
     │                        │     For each seatId:  │                   │
     │                        │       SETNX "lock:10:{seatId}" TTL=30s   │
     │                        ├──────────────────────────────────────────>│
     │                        │     If lock fail → ConflictException     │
     │                        │     (tránh deadlock bằng sorted order)   │
     │                        │                      │                    │
     │                        │ 11. RE-VERIFY holds under lock (TOCTOU)  │
     │                        │     assertHeldByUser() lần nữa           │
     │                        ├──────────────────────────────────────────>│
     │                        │                      │                    │
     │                        │ 12. @Transactional createBookingTransaction()
     │                        │     a. Check DB: Ghế chưa bị booking?    │
     │                        │        SELECT * FROM booking_seats       │
     │                        │        WHERE showtime_id=10              │
     │                        │        AND seat_id IN (1,2,3)            │
     │                        │        AND booking.status IN (PENDING, CONFIRMED)
     │                        ├─────────────────────>│                    │
     │                        │     If exists → ConflictException        │
     │                        │                      │                    │
     │                        │     b. Calculate price:                  │
     │                        │        STANDARD: basePrice               │
     │                        │        VIP: basePrice * 1.3              │
     │                        │                      │                    │
     │                        │     c. INSERT INTO bookings              │
     │                        │        (account_id, showtime_id,         │
     │                        │         total_price, status=PENDING_PAYMENT)
     │                        ├─────────────────────>│                    │
     │                        │                      │                    │
     │                        │     d. INSERT INTO booking_seats         │
     │                        │        (booking_id, seat_id, price)      │
     │                        ├─────────────────────>│                    │
     │                        │                      │                    │
     │                        │ 13. CONSUME Redis holds                  │
     │                        │     DELETE hold:10:1, hold:10:2, hold:10:3
     │                        ├──────────────────────────────────────────>│
     │                        │     (Ghế đã persist vào DB)              │
     │                        │                      │                    │
     │                        │ 14. RELEASE locks (finally block)        │
     │                        │     DELETE lock:10:1, lock:10:2, lock:10:3
     │                        ├──────────────────────────────────────────>│
     │                        │                      │                    │
     │  200 OK                │                      │                    │
     │  {bookingId, totalPrice, status: PENDING_PAYMENT}                │
     │  "Complete payment within 15 minutes"        │                    │
     │<───────────────────────┤                      │                    │
     │                        │                      │                    │
     │ User phải thanh toán trong 15 phút!          │                    │
```

#### **STEP 3: Payment** (Mock - cần integrate)

```
┌─────────┐              ┌─────────┐            ┌──────────┐         ┌───────────┐
│  User   │              │ Backend │            │  MySQL   │         │  Gateway  │
└────┬────┘              └────┬────┘            └────┬─────┘         └─────┬─────┘
     │                        │                      │                      │
     │ 15. Frontend redirect sang payment           │                      │
     │ POST /api/payments/create/{bookingId}        │                      │
     ├───────────────────────>│                      │                      │
     │                        │                      │                      │
     │                        │ 16. Validate booking │                      │
     │                        │     status=PENDING_PAYMENT?                │
     │                        ├─────────────────────>│                      │
     │                        │                      │                      │
     │                        │ 17. Generate payment URL (⚠️ MOCK!)        │
     │                        │     TODO: Integrate VNPay/MoMo/Stripe      │
     │                        │                      │                      │
     │  200 OK                │                      │                      │
     │  {paymentUrl: "https://mock-payment.com/..."}│                      │
     │<───────────────────────┤                      │                      │
     │                        │                      │                      │
     │ 18. User redirect sang gateway               │                      │
     ├──────────────────────────────────────────────────────────────────────>│
     │                        │                      │                      │
     │ 19. User thanh toán    │                      │                      │
     ├<─────────────────────────────────────────────────────────────────────┤
     │                        │                      │                      │
     │ 20. Gateway callback   │                      │                      │
     │ POST /api/payments/callback                  │                      │
     ├───────────────────────────────────────────────────────────────────────>│
     │                        │                      │                      │
     │                        │ 21. ⚠️ TODO: Verify signature!             │
     │                        │     (CRITICAL SECURITY!)                   │
     │                        │                      │                      │
     │                        │ 22. Validate amount matches booking.totalPrice
     │                        ├─────────────────────>│                      │
     │                        │                      │                      │
     │                        │ 23. UPDATE booking   │                      │
     │                        │     status = CONFIRMED                     │
     │                        ├─────────────────────>│                      │
     │                        │                      │                      │
     │                        │ 24. TODO: Send email confirmation          │
     │                        │ 25. TODO: Generate QR code for ticket      │
     │                        │                      │                      │
     │  200 OK                │                      │                      │
     │  "Payment successful"  │                      │                      │
     │<───────────────────────┤                      │                      │
```

#### **STEP 4: Auto-Expire (Nếu không thanh toán)**

```
┌────────────┐           ┌─────────┐            ┌──────────┐         ┌───────┐
│ Cron Job   │           │ Service │            │  MySQL   │         │ Redis │
└─────┬──────┘           └────┬────┘            └────┬─────┘         └───┬───┘
      │                       │                      │                    │
      │ @Scheduled(cron="0 */5 * * * *")            │                    │
      │ Chạy mỗi 5 phút       │                      │                    │
      ├──────────────────────>│                      │                    │
      │                       │                      │                    │
      │                       │ 1. Find expired bookings                  │
      │                       │    SELECT * FROM bookings                 │
      │                       │    WHERE status = 'PENDING_PAYMENT'       │
      │                       │    AND booking_date < NOW() - 15 minutes  │
      │                       ├─────────────────────>│                    │
      │                       │                      │                    │
      │                       │ 2. For each booking: │                    │
      │                       │    UPDATE status = EXPIRED                │
      │                       ├─────────────────────>│                    │
      │                       │                      │                    │
      │                       │ 3. Release Redis holds (nếu còn)          │
      │                       │    DELETE hold:10:1, hold:10:2, ...       │
      │                       ├──────────────────────────────────────────>│
      │                       │                      │                    │
      │                       │ Ghế đã available cho user khác!           │
```

### ⚡ Key Mechanisms

#### 1. **Redis Distributed Lock**
```java
// Sorted order để tránh deadlock
List<Long> sortedSeatIds = seatIds.stream().sorted().toList();
for (Long seatId : sortedSeatIds) {
    redis.opsForValue().setIfAbsent(
        "lock:%d:%d".formatted(showtimeId, seatId),
        "locked",
        30, TimeUnit.SECONDS
    );
}
```

#### 2. **TOCTOU Prevention**
```
Time-of-Check, Time-of-Use race condition:
1. Check holds (OK)
2. [Race: Another request steals seat]
3. Create booking (FAIL)

Solution:
1. Pre-check holds
2. Acquire locks
3. RE-CHECK holds under lock
4. Create booking
```

#### 3. **Atomic Hold**
```java
Boolean success = redis.opsForValue().setIfAbsent(key, userId, ttl);
if (!success) {
    // Ghế đã bị hold
    String currentOwner = redis.opsForValue().get(key);
    if (userId.equals(currentOwner)) {
        // Idempotent: Refresh TTL
        redis.expire(key, ttl);
    } else {
        throw new ConflictException("Seat held by another user");
    }
}
```

### 🧪 Test Cases

#### Happy Path (End-to-End)
```bash
# 1. Hold seats
POST http://localhost:8080/api/seats/hold
Authorization: Bearer {accessToken}
{
  "showtimeId": 10,
  "seatIds": [1, 2, 3],
  "ttlSec": 120
}
Expected: 200 OK

# 2. Create booking (trong 120s)
POST http://localhost:8080/api/bookings
Authorization: Bearer {accessToken}
{
  "showtimeId": 10,
  "seatIds": [1, 2, 3]
}
Expected: 200 OK
Response: {
  "id": 100,
  "totalPrice": 450000,
  "status": "PENDING_PAYMENT"
}

# 3. Create payment (trong 15 phút)
POST http://localhost:8080/api/payments/create/100
Authorization: Bearer {accessToken}
Expected: 200 OK
Response: {paymentUrl: "https://..."}

# 4. Callback (mock payment success)
POST http://localhost:8080/api/payments/callback
{
  "bookingId": 100,
  "status": "SUCCESS",
  "transactionId": "TXN123"
}
Expected: 200 OK, booking.status = CONFIRMED
```

#### Error Cases - Race Conditions

**Test 1: Concurrent Hold**
```bash
# User A hold ghế
POST /api/seats/hold {seatIds: [1]}
Expected: 200 OK

# User B hold cùng ghế (concurrent)
POST /api/seats/hold {seatIds: [1]}
Expected: 409 Conflict, "Seat held by another user"
```

**Test 2: Hold Expired**
```bash
# 1. Hold ghế
POST /api/seats/hold {seatIds: [1], ttlSec: 10}

# 2. Đợi > 10 giây

# 3. Create booking
POST /api/bookings {seatIds: [1]}
Expected: 409 Conflict, "Seat no longer held (expired)"
```

**Test 3: Double Booking (DB Check)**
```bash
# User A tạo booking thành công
POST /api/bookings {seatIds: [1]}
Expected: 200 OK, booking_id=1

# User B hold ghế (Redis cho phép vì hold đã consumed)
POST /api/seats/hold {seatIds: [1]}
Expected: 200 OK (Redis SETNX success)

# User B tạo booking
POST /api/bookings {seatIds: [1]}
Expected: 409 Conflict, "Seat already booked in DB"
(Vì DB check trong transaction)
```

**Test 4: Payment Timeout**
```bash
# 1. Tạo booking lúc 10:00
POST /api/bookings
Response: bookingId=1, status=PENDING_PAYMENT

# 2. Không thanh toán

# 3. Sau 15 phút (10:15), cron job chạy
# Booking tự động EXPIRED

# 4. Check booking status
GET /api/bookings/1
Expected: status=EXPIRED
```

### ⚠️ Critical Issues

1. **⚠️ Không có idempotency check cho payment callback**
   - Attacker có thể gọi callback nhiều lần
   - Fix: Check `transactionId` unique trong DB

2. **⚠️ Không verify payment signature**
   - Attacker có thể fake payment success
   - Fix: Implement HMAC-SHA512 signature verification

3. **⚠️ Amount validation thiếu**
   - Phải verify `receivedAmount == booking.totalPrice`

4. **⚠️ Lock timeout hardcoded (30s)**
   - Nên configure trong application.yml

---

## 6. 💳 Payment Flow (Mock)

### ⚠️ CRITICAL: Chưa integrate thật

**Current Status:**
- ❌ Payment URL là mock
- ❌ Không verify signature
- ❌ Không có webhook
- ❌ Không check idempotency

**TODO:**
```java
// 1. Choose gateway: VNPay, MoMo, Stripe

// 2. Generate signed payment URL
String vnpUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
Map<String, String> params = buildPaymentParams(booking);
String signature = HmacSHA512(secretKey, params);
params.put("vnp_SecureHash", signature);
return vnpUrl + "?" + buildQueryString(params);

// 3. Verify callback signature
boolean verifySignature(PaymentRequest req) {
    String receivedHash = req.getSignature();
    String calculatedHash = HmacSHA512(secretKey, buildSignData(req));
    return receivedHash.equals(calculatedHash);
}

// 4. Add webhook endpoint
@PostMapping("/webhook")
public ResponseEntity<?> webhook(@RequestBody String payload, 
                                 @RequestHeader("Signature") String sig) {
    if (!verifyWebhookSignature(payload, sig)) {
        return ResponseEntity.status(401).build();
    }
    // Process payment...
}
```

---

## 7. ⏰ Booking Expiration Flow (Auto)

### 📖 Mô tả
Cron job tự động expire các booking PENDING_PAYMENT quá 15 phút.

### 🔄 Flow
```java
@Scheduled(cron = "0 */5 * * * *")  // Mỗi 5 phút
public void expireBookings() {
    // 1. Find expired bookings
    List<Booking> expired = bookingRepository.findAllExpiredBookings(
        BookingStatus.PENDING_PAYMENT,
        LocalDateTime.now().minusMinutes(15)
    );
    
    // 2. Update status = EXPIRED
    // 3. Release Redis holds
    // 4. Seats available cho user khác
}
```

### 🧪 Test
```bash
# 1. Tạo booking
POST /api/bookings
Response: bookingId=1, booking_date=10:00

# 2. Đợi 16 phút (hoặc modify booking_date trong DB)

# 3. Trigger cron manual (hoặc đợi 5 phút)

# 4. Check status
GET /api/bookings/1
Expected: status=EXPIRED

# 5. Try hold lại ghế đã expired
POST /api/seats/hold {seatIds: [1]}
Expected: 200 OK (ghế đã available)
```

---

## 📊 Summary: Flow Dependencies

```
Registration → Activation → Login → Browse Movies
                                  ↓
                            Select Showtime
                                  ↓
                              Hold Seats (120s)
                                  ↓
                            Create Booking (PENDING_PAYMENT)
                                  ↓
                              ┌───┴───┐
                              │       │
                        Create Payment │
                              │       │
                              ↓       ↓
                     Payment Success  Payment Timeout (15 min)
                              │       │
                        CONFIRMED    EXPIRED (Cron)
                              │       │
                        Send Email   Release Seats
```

---

## 🎯 Next Steps

1. ✅ Đọc [SEQUENCE_DIAGRAMS.md](./SEQUENCE_DIAGRAMS.md) để xem biểu đồ chi tiết
2. 🐛 Đọc [ISSUES.md](./ISSUES.md) để biết các bugs cần fix
3. 🧪 Import Postman collection để test
4. 🔐 Implement payment gateway integration (Priority: HIGH)
5. ✅ Add comprehensive error handling
6. 📧 Improve email templates

---

**📝 Note:** Document này dành cho **middle-level developers**. Nếu bạn là junior, hãy đọc từng flow một và test trên Postman trước khi đọc code.
