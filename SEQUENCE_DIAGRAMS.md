# 📊 Sequence Diagrams - Movie Booking System

> Biểu đồ tuần tự (Sequence Diagrams) cho tất cả các flows trong hệ thống

## 📋 Danh sách Diagrams

1. [System Overview Diagram](#1-system-overview-diagram)
2. [Registration & Activation Flow](#2-registration--activation-flow)
3. [Login & JWT Authentication Flow](#3-login--jwt-authentication-flow)
4. [Forgot & Reset Password Flow](#4-forgot--reset-password-flow)
5. [Booking Flow - Complete (CRITICAL)](#5-booking-flow---complete-critical)
6. [Payment Flow](#6-payment-flow)
7. [Auto Booking Expiration](#7-auto-booking-expiration)
8. [Movie Search & Browse](#8-movie-search--browse)

---

## 1. System Overview Diagram

### Tổng quan kiến trúc và luồng chính

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant API as Spring Boot API
    participant SEC as Security Filter
    participant SVC as Services
    participant REDIS as Redis
    participant DB as MySQL
    participant EMAIL as Email Service

    Note over U,EMAIL: SYSTEM ARCHITECTURE OVERVIEW

    rect rgb(200, 220, 240)
    Note right of U: 1. AUTHENTICATION FLOW
    U->>+FE: 1.1 Register/Login
    FE->>+API: POST /api/auth/*
    API->>+SEC: JWT Filter
    SEC->>+SVC: AuthService
    SVC->>+DB: Validate credentials
    DB-->>-SVC: Account data
    SVC->>+REDIS: Store refresh token
    REDIS-->>-SVC: OK
    SVC-->>-SEC: JWT tokens
    SEC-->>-API: Authenticated
    API-->>-FE: Access + Refresh token
    FE-->>-U: Success
    end

    rect rgb(220, 240, 200)
    Note right of U: 2. BOOKING FLOW (CRITICAL)
    U->>+FE: 2.1 Select seats
    FE->>+API: POST /api/seats/hold
    API->>+REDIS: SETNX hold keys (TTL 120s)
    REDIS-->>-API: Seats held
    API-->>-FE: Success (120s timer)
    
    U->>+FE: 2.2 Create booking
    FE->>+API: POST /api/bookings
    API->>+REDIS: Acquire locks
    REDIS-->>-API: Locked
    API->>+DB: Check availability
    DB-->>-API: Available
    API->>+DB: INSERT booking
    DB-->>-API: Booking created
    API->>+REDIS: Consume holds
    REDIS-->>-API: Holds deleted
    API->>+REDIS: Release locks
    REDIS-->>-API: Locks released
    API-->>-FE: Booking (PENDING_PAYMENT)
    FE-->>-U: Redirect to payment
    end

    rect rgb(240, 220, 200)
    Note right of U: 3. PAYMENT FLOW
    U->>+FE: 3.1 Pay
    FE->>+API: POST /api/payments/create
    API-->>-FE: Payment URL
    FE->>+U: Redirect to gateway
    U->>U: Complete payment
    U->>+API: Callback /api/payments/callback
    API->>+DB: UPDATE booking status
    DB-->>-API: CONFIRMED
    API->>+EMAIL: Send confirmation
    EMAIL-->>-API: Email sent
    API-->>-U: Success
    end

    rect rgb(240, 200, 220)
    Note right of U: 4. AUTO-EXPIRATION (Background)
    loop Every 5 minutes
        SVC->>+DB: Find expired bookings
        DB-->>-SVC: Expired list
        SVC->>+DB: UPDATE status=EXPIRED
        DB-->>-SVC: Updated
        SVC->>+REDIS: Release holds
        REDIS-->>-SVC: Released
    end
    end
```

---

## 2. Registration & Activation Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as AuthController
    participant SVC as AuthService
    participant DB as MySQL
    participant REDIS as Redis
    participant OTP as OtpService
    participant MAIL as MailService

    Note over U,MAIL: REGISTRATION & ACTIVATION FLOW

    rect rgb(200, 220, 240)
    Note right of U: STEP 1: REGISTRATION
    U->>+API: POST /api/auth/register
    Note right of U: {username, email, password,<br/>firstName, lastName, phoneNumber}
    
    API->>+SVC: register(request)
    
    SVC->>+DB: Check username exists?
    DB-->>-SVC: Not exists
    
    SVC->>+DB: Check email exists?
    DB-->>-SVC: Not exists
    
    Note over SVC: Hash password (BCrypt)
    
    SVC->>+DB: INSERT INTO accounts
    Note right of SVC: status=ACTIVE<br/>email_verified=false
    DB-->>-SVC: Account created (ID: 123)
    
    SVC->>+DB: INSERT INTO user (profile)
    DB-->>-SVC: User created
    
    SVC->>+OTP: sendOtp(email, REGISTER)
    
    OTP->>+REDIS: Generate OTP (6 digits)
    Note right of OTP: Key: "otp:REGISTER:{email}"<br/>TTL: 5 minutes
    REDIS-->>-OTP: OTP stored
    
    OTP->>+MAIL: Send email
    Note right of MAIL: Template: registration-otp.html<br/>OTP: 123456
    MAIL-->>-OTP: Email sent
    OTP-->>-SVC: OTP sent
    
    SVC-->>-API: Registration successful
    API-->>-U: 200 OK
    Note left of API: "Check your email for OTP"
    end

    rect rgb(220, 240, 200)
    Note right of U: STEP 2: ACTIVATION
    U->>U: Check email → Get OTP
    
    U->>+API: POST /api/auth/activate
    Note right of U: {email, otp: "123456"}
    
    API->>+SVC: activateAccount(request)
    
    SVC->>+OTP: verifyOtp(email, otp, REGISTER)
    
    OTP->>+REDIS: GET otp:REGISTER:{email}
    REDIS-->>-OTP: "123456"
    
    alt OTP valid & not expired
        OTP-->>SVC: true
        
        SVC->>+DB: UPDATE accounts<br/>SET email_verified=true<br/>WHERE email=?
        DB-->>-SVC: Updated
        
        SVC->>+OTP: deleteOtp(email, REGISTER)
        OTP->>+REDIS: DEL otp:REGISTER:{email}
        REDIS-->>-OTP: Deleted
        OTP-->>-SVC: OTP deleted
        
        SVC-->>-API: Account activated
        API-->>-U: 200 OK
        Note left of API: "Account activated successfully"
        
    else OTP invalid or expired
        OTP-->>SVC: false
        SVC-->>API: BadRequestException
        API-->>U: 400 Bad Request
        Note left of API: "Invalid or expired OTP"
    end
    end
```

---

## 3. Login & JWT Authentication Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as AuthController
    participant SEC as Spring Security
    participant SVC as AuthService
    participant JWT as JwtProvider
    participant DB as MySQL
    participant REDIS as Redis

    Note over U,REDIS: LOGIN & JWT AUTHENTICATION FLOW

    rect rgb(200, 220, 240)
    Note right of U: LOGIN
    U->>+API: POST /api/auth/login
    Note right of U: {username, password}
    
    API->>+SVC: login(request)
    
    SVC->>+SEC: authenticate(username, password)
    
    SEC->>+DB: Load user by username
    DB-->>-SEC: Account + Roles
    
    SEC->>SEC: Compare password (BCrypt)
    
    alt Authentication successful
        SEC-->>-SVC: Authentication object
        
        SVC->>SVC: Validate email_verified=true
        SVC->>SVC: Validate status=ACTIVE
        
        SVC->>+JWT: generateToken(account)
        Note right of JWT: Claims: {sub, roles, iat, exp}<br/>TTL: 30 minutes<br/>Algorithm: HMAC-SHA256
        JWT-->>-SVC: Access Token
        
        SVC->>+JWT: generateRefreshToken(account)
        Note right of JWT: TTL: 1 day
        JWT-->>-SVC: Refresh Token
        
        SVC->>+REDIS: Store refresh token
        Note right of SVC: Key: "auth:refreshToken:{username}"<br/>Value: refreshToken<br/>TTL: 1 day
        REDIS-->>-SVC: Stored
        
        SVC-->>-API: AuthResponse(accessToken, refreshToken)
        API-->>-U: 200 OK
        Note left of API: {accessToken, refreshToken}
        
    else Authentication failed
        SEC-->>SVC: AuthenticationException
        SVC-->>API: BadRequestException
        API-->>U: 400 Bad Request
        Note left of API: "Invalid username or password"
    end
    end

    rect rgb(220, 240, 200)
    Note right of U: AUTHENTICATED REQUEST
    U->>+API: GET /api/bookings
    Note right of U: Header: Authorization: Bearer {accessToken}
    
    API->>+SEC: JwtFilter.doFilterInternal()
    
    SEC->>+JWT: validateToken(accessToken)
    JWT->>JWT: Verify signature
    JWT->>JWT: Check expiration
    JWT-->>-SEC: Valid
    
    SEC->>+JWT: extractUsername(accessToken)
    JWT-->>-SEC: "username"
    
    SEC->>+DB: Load user by username
    DB-->>-SEC: Account + Roles
    
    SEC->>SEC: Set SecurityContext
    SEC-->>-API: Authenticated
    
    API->>API: Execute controller method
    API-->>-U: 200 OK (response data)
    end

    rect rgb(240, 220, 200)
    Note right of U: REFRESH TOKEN
    U->>+API: POST /api/auth/refresh-token
    Note right of U: {refreshToken}
    
    API->>+SVC: refreshToken(refreshToken)
    
    SVC->>+JWT: validateToken(refreshToken)
    JWT-->>-SVC: Valid
    
    SVC->>+JWT: extractUsername(refreshToken)
    JWT-->>-SVC: "username"
    
    SVC->>+REDIS: GET auth:refreshToken:{username}
    REDIS-->>-SVC: Stored refresh token
    
    SVC->>SVC: Compare tokens (match?)
    
    alt Tokens match
        SVC->>+JWT: generateToken(account)
        Note right of JWT: New access token
        JWT-->>-SVC: New Access Token
        
        SVC-->>-API: AuthResponse(newAccessToken, refreshToken)
        API-->>-U: 200 OK
        Note left of API: {accessToken (new), refreshToken (same)}
        
    else Tokens don't match
        SVC-->>API: BadRequestException
        API-->>U: 400 Bad Request
        Note left of API: "Invalid refresh token"
    end
    end
```

---

## 4. Forgot & Reset Password Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as AuthController
    participant SVC as PassWordService
    participant OTP as OtpService
    participant REDIS as Redis
    participant DB as MySQL
    participant MAIL as MailService

    Note over U,MAIL: FORGOT & RESET PASSWORD FLOW

    rect rgb(200, 220, 240)
    Note right of U: STEP 1: REQUEST RESET
    U->>+API: POST /api/auth/forgot-password
    Note right of U: {email}
    
    API->>+SVC: forgotPassword(request)
    
    SVC->>+DB: Check email exists?
    DB-->>-SVC: Account found
    
    SVC->>+OTP: sendOtp(email, FORGOT_PASSWORD)
    
    OTP->>+REDIS: Generate OTP
    Note right of OTP: Key: "otp:FORGOT_PASSWORD:{email}"<br/>TTL: 5 minutes
    REDIS-->>-OTP: OTP stored
    
    OTP->>+MAIL: Send reset email
    Note right of MAIL: Subject: "Reset your password"<br/>OTP: 789012
    MAIL-->>-OTP: Email sent
    OTP-->>-SVC: OTP sent
    
    SVC-->>-API: Success
    API-->>-U: 200 OK
    Note left of API: "Check your email"
    end

    rect rgb(220, 240, 200)
    Note right of U: STEP 2: RESET PASSWORD
    U->>U: Check email → Get OTP
    
    U->>+API: POST /api/auth/reset-password
    Note right of U: {email, otp, newPassword}
    
    API->>+SVC: resetPassword(request)
    
    SVC->>+OTP: verifyOtp(email, otp, FORGOT_PASSWORD)
    
    OTP->>+REDIS: GET otp:FORGOT_PASSWORD:{email}
    REDIS-->>-OTP: "789012"
    
    alt OTP valid
        OTP-->>-SVC: true
        
        SVC->>+DB: Load account by email
        DB-->>-SVC: Account
        
        Note over SVC: Hash new password (BCrypt)
        
        SVC->>+DB: UPDATE accounts<br/>SET password=?<br/>WHERE email=?
        DB-->>-SVC: Password updated
        
        SVC->>+OTP: deleteOtp(email, FORGOT_PASSWORD)
        OTP->>+REDIS: DEL otp:FORGOT_PASSWORD:{email}
        REDIS-->>-OTP: Deleted
        OTP-->>-SVC: OTP deleted
        
        SVC-->>-API: Password reset successful
        API-->>-U: 200 OK
        Note left of API: "Password reset successfully"
        
    else OTP invalid
        OTP-->>SVC: false
        SVC-->>API: BadRequestException
        API-->>U: 400 Bad Request
        Note left of API: "Invalid or expired OTP"
    end
    end
```

---

## 5. Booking Flow - Complete (CRITICAL)

### 🎯 Flow phức tạp nhất - Xử lý concurrent booking

```mermaid
sequenceDiagram
    participant U as User
    participant API as Controllers
    participant SVC as BookingService
    participant SEAT as SeatDomainService
    participant LOCK as RedisLockService
    participant REDIS as Redis
    participant DB as MySQL

    Note over U,DB: COMPLETE BOOKING FLOW WITH CONCURRENCY CONTROL

    rect rgb(200, 220, 240)
    Note right of U: STEP 1: HOLD SEATS
    U->>+API: POST /api/seats/hold
    Note right of U: {showtimeId: 10,<br/>seatIds: [1,2,3],<br/>ttlSec: 120}
    
    API->>+DB: Validate showtime exists
    DB-->>-API: Showtime OK
    
    API->>+DB: Validate seats exist
    DB-->>-API: Seats OK
    
    API->>+SEAT: holdSeats(showtimeId, seatIds, userId, 120s)
    
    loop For each seatId in [1,2,3]
        SEAT->>+REDIS: SETNX hold:10:1 = userId
        Note right of SEAT: TTL: 120 seconds
        
        alt Seat not held
            REDIS-->>SEAT: Success (key created)
            Note left of REDIS: Seat locked
            
        else Seat already held
            REDIS-->>SEAT: Fail (key exists)
            SEAT->>+REDIS: GET hold:10:1
            REDIS-->>-SEAT: currentOwner
            
            alt Same user (idempotent)
                SEAT->>+REDIS: EXPIRE hold:10:1 120
                Note right of SEAT: Refresh TTL
                REDIS-->>-SEAT: TTL refreshed
                
            else Different user
                SEAT-->>API: ConflictException
                API-->>U: 409 Conflict
                Note left of API: "Seat held by another user"
                Note over U,DB: FLOW STOPS
            end
        end
    end
    
    SEAT-->>-API: All seats held
    API-->>-U: 200 OK
    Note left of API: "Seats held for 120 seconds"
    Note over U: User has 120s to create booking
    end

    rect rgb(220, 240, 200)
    Note right of U: STEP 2: CREATE BOOKING
    U->>+API: POST /api/bookings
    Note right of U: {showtimeId: 10,<br/>seatIds: [1,2,3]}
    
    API->>+SVC: create(request)
    
    Note over SVC: 1. Validate input
    SVC->>SVC: Check seatIds not empty
    
    SVC->>+DB: Load showtime
    DB-->>-SVC: Showtime object
    
    Note over SVC: 2. PRE-CHECK: Verify holds
    SVC->>+SEAT: assertHeldByUser(10, [1,2,3], userId)
    
    loop For each seatId
        SEAT->>+REDIS: GET hold:10:1
        REDIS-->>-SEAT: owner
        
        alt Not held or wrong owner
            SEAT-->>SVC: ConflictException
            SVC-->>API: ConflictException
            API-->>U: 409 Conflict
            Note left of API: "Seat not held or expired"
            Note over U,DB: FLOW STOPS
        end
    end
    SEAT-->>-SVC: All seats held by user ✓
    
    Note over SVC: 3. Get seat infos
    SVC->>+SEAT: getSeatInfos([1,2,3])
    SEAT->>+DB: SELECT * FROM seats WHERE id IN (1,2,3)
    DB-->>-SEAT: [Seat(id:1, type:VIP), Seat(id:2, type:STANDARD), ...]
    SEAT-->>-SVC: [SeatInfo(1, VIP), SeatInfo(2, STANDARD), ...]
    
    Note over SVC: 4. ACQUIRE DISTRIBUTED LOCKS
    SVC->>SVC: Sort seatIds: [1,2,3]
    
    loop For each seatId (sorted)
        SVC->>+LOCK: tryLockSeat(10, 1, 30s)
        LOCK->>+REDIS: SETNX lock:10:1 = "locked"
        Note right of LOCK: TTL: 30 seconds
        
        alt Lock acquired
            REDIS-->>-LOCK: Success
            LOCK-->>-SVC: true
            Note over SVC: Lock acquired for seat 1
            
        else Lock failed
            REDIS-->>LOCK: Fail
            LOCK-->>SVC: false
            
            Note over SVC: Release all acquired locks
            SVC->>+LOCK: Release locks [1]
            LOCK->>+REDIS: DEL lock:10:1
            REDIS-->>-LOCK: Deleted
            LOCK-->>-SVC: Locks released
            
            SVC-->>API: ConflictException
            API-->>U: 409 Conflict
            Note left of API: "Cannot lock seat, try again"
            Note over U,DB: FLOW STOPS
        end
    end
    
    Note over SVC: All locks acquired ✓
    
    Note over SVC: 5. RE-VERIFY holds (TOCTOU prevention)
    SVC->>+SEAT: assertHeldByUser(10, [1,2,3], userId)
    SEAT->>REDIS: Verify holds again under lock
    SEAT-->>-SVC: Still held ✓
    
    Note over SVC: 6. @Transactional createBookingTransaction()
    SVC->>+DB: BEGIN TRANSACTION
    
    SVC->>+DB: Check DB: Seats not booked?
    Note right of SVC: SELECT * FROM booking_seats<br/>WHERE showtime_id=10 AND seat_id IN (1,2,3)<br/>AND booking.status IN (PENDING, CONFIRMED)
    
    alt Seats already booked
        DB-->>SVC: Found booked seats [1]
        SVC->>+DB: ROLLBACK
        DB-->>-SVC: Rolled back
        
        Note over SVC: Release locks (finally)
        SVC->>+LOCK: Release locks
        LOCK->>+REDIS: DEL lock:10:1, lock:10:2, lock:10:3
        REDIS-->>-LOCK: Deleted
        LOCK-->>-SVC: Released
        
        SVC-->>API: ConflictException
        API-->>U: 409 Conflict
        Note left of API: "Seat already booked (race condition)"
        Note over U,DB: FLOW STOPS
    else Seats available
        DB-->>-SVC: No conflicts
        
        Note over SVC: Calculate prices
        Note right of SVC: STANDARD: basePrice<br/>VIP: basePrice * 1.3
        SVC->>SVC: totalPrice = sum(prices)
        
        SVC->>+DB: INSERT INTO bookings
        Note right of SVC: (account_id, showtime_id,<br/>total_price, status=PENDING_PAYMENT)
        DB-->>-SVC: Booking created (ID: 100)
        
        SVC->>+DB: INSERT INTO booking_seats
        Note right of SVC: (booking_id:100, seat_id:1, price:195000)<br/>(booking_id:100, seat_id:2, price:150000)<br/>(booking_id:100, seat_id:3, price:150000)
        DB-->>-SVC: BookingSeats created
        
        SVC->>+DB: COMMIT
        DB-->>-SVC: Transaction committed
        
        Note over SVC: 7. CONSUME Redis holds
        SVC->>+SEAT: consumeHoldToBooked(10, [1,2,3])
        
        loop For each seatId
            SEAT->>+REDIS: DEL hold:10:1
            REDIS-->>-SEAT: Deleted
            Note right of SEAT: Hold consumed (booked in DB)
        end
        SEAT-->>-SVC: Holds consumed ✓
        
        Note over SVC: 8. RELEASE locks (finally)
        SVC->>+LOCK: releaseSeatLock(10, 1)
        LOCK->>+REDIS: DEL lock:10:1
        REDIS-->>-LOCK: Deleted
        LOCK-->>-SVC: Lock 1 released
        
        SVC->>+LOCK: releaseSeatLock(10, 2)
        LOCK->>+REDIS: DEL lock:10:2
        REDIS-->>-LOCK: Deleted
        LOCK-->>-SVC: Lock 2 released
        
        SVC->>+LOCK: releaseSeatLock(10, 3)
        LOCK->>+REDIS: DEL lock:10:3
        REDIS-->>-LOCK: Deleted
        LOCK-->>-SVC: Lock 3 released
        
        SVC-->>-API: BookingResponse(id:100, status:PENDING_PAYMENT)
        API-->>-U: 200 OK
        Note left of API: {id: 100, totalPrice: 495000,<br/>status: PENDING_PAYMENT}<br/>"Complete payment within 15 minutes"
    end
    end

    Note over U: User must pay within 15 minutes!
```

### 🔒 Concurrency Control Mechanisms

**1. Redis SETNX (Atomic Hold)**
```
SETNX hold:{showtimeId}:{seatId} {userId} TTL=120s
- Atomic operation
- Only 1 user can hold at a time
- Auto-expire after 120s
```

**2. Distributed Locks (Deadlock Prevention)**
```
Sorted order: [1, 2, 3]
- Always lock in ascending order
- Prevents circular wait (deadlock condition)
- Timeout: 30s per lock
```

**3. TOCTOU Prevention**
```
Time-of-Check, Time-of-Use race:
1. Pre-check holds (before lock)
2. Acquire locks
3. RE-CHECK holds (under lock) ← CRITICAL!
4. Create booking
```

**4. Database Transaction Isolation**
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
- Check seats not booked in DB
- Insert booking + booking_seats
- Commit atomically
```

---

## 6. Payment Flow

```mermaid
sequenceDiagram
    participant U as User
    participant API as PaymentController
    participant SVC as PaymentService
    participant DB as MySQL
    participant SEAT as SeatDomainService
    participant REDIS as Redis
    participant GW as Payment Gateway

    Note over U,GW: PAYMENT FLOW (⚠️ MOCK - Need Integration)

    rect rgb(200, 220, 240)
    Note right of U: STEP 1: CREATE PAYMENT
    U->>+API: POST /api/payments/create/100
    Note right of U: bookingId: 100
    
    API->>+SVC: createPaymentUrl(100)
    
    SVC->>+DB: Load booking
    DB-->>-SVC: Booking(id:100, status:PENDING_PAYMENT, totalPrice:495000)
    
    alt Status != PENDING_PAYMENT
        SVC-->>API: BadRequestException
        API-->>U: 400 Bad Request
        Note left of API: "Booking not in PENDING_PAYMENT status"
        Note over U,GW: FLOW STOPS
    end
    
    Note over SVC: ⚠️ TODO: Generate real payment URL
    Note right of SVC: Should integrate:<br/>- VNPay: vnpayment.vn<br/>- MoMo: momo.vn<br/>- Stripe: stripe.com
    
    Note over SVC: MOCK URL Generation
    SVC->>SVC: mockUrl = "https://mock-gateway.com/checkout?bookingId=100&amount=495000"
    
    SVC-->>-API: paymentUrl
    API-->>-U: 200 OK
    Note left of API: {paymentUrl: "https://mock-gateway.com/..."}
    
    U->>+GW: Redirect to payment gateway
    Note right of U: User fills card info, confirms
    
    GW->>GW: Process payment
    end

    rect rgb(220, 240, 200)
    Note right of GW: STEP 2: PAYMENT CALLBACK
    GW->>+API: POST /api/payments/callback
    Note right of GW: {bookingId: 100,<br/>status: "SUCCESS",<br/>transactionId: "TXN123",<br/>amount: 495000,<br/>signature: "abc..."}
    
    API->>+SVC: handlePaymentCallback(request)
    
    Note over SVC: ⚠️ CRITICAL: Verify signature
    Note right of SVC: TODO: Implement HMAC-SHA512<br/>verification with gateway secret
    
    Note over SVC: ⚠️ Missing implementation:
    Note right of SVC: if (!verifySignature(request)) {<br/>  throw SecurityException;<br/>}
    
    SVC->>+DB: Load booking
    DB-->>-SVC: Booking(id:100)
    
    Note over SVC: ⚠️ TODO: Check idempotency
    Note right of SVC: Check transactionId unique:<br/>if (paymentRepo.existsByTxnId(TXN123)) {<br/>  return "DUPLICATE";<br/>}
    
    Note over SVC: ⚠️ TODO: Validate amount
    Note right of SVC: if (request.amount != booking.totalPrice) {<br/>  throw BadRequestException;<br/>}
    
    alt Payment SUCCESS
        SVC->>+DB: BEGIN TRANSACTION
        
        SVC->>+DB: UPDATE bookings<br/>SET status='CONFIRMED'<br/>WHERE id=100
        DB-->>-SVC: Updated
        
        SVC->>+DB: COMMIT
        DB-->>-SVC: Committed
        
        Note over SVC: Consume Redis holds
        SVC->>+SEAT: consumeHoldToBooked(10, [1,2,3])
        SEAT->>+REDIS: DEL hold:10:1, hold:10:2, hold:10:3
        REDIS-->>-SEAT: Deleted
        SEAT-->>-SVC: Holds consumed
        
        Note over SVC: ⚠️ TODO: Send email
        Note right of SVC: emailService.sendBookingConfirmation(booking)
        
        Note over SVC: ⚠️ TODO: Generate QR code
        Note right of SVC: qrCode = qrService.generate(bookingId)
        
        SVC-->>-API: PaymentResponse(SUCCESS)
        API-->>-GW: 200 OK
        GW-->>-U: Payment successful!
        Note right of U: Show confirmation page
        
    else Payment FAILED
        SVC->>+DB: BEGIN TRANSACTION
        
        SVC->>+DB: UPDATE bookings<br/>SET status='CANCELLED'<br/>WHERE id=100
        DB-->>-SVC: Updated
        
        SVC->>+DB: COMMIT
        DB-->>-SVC: Committed
        
        Note over SVC: Release holds
        SVC->>+SEAT: releaseHolds(10, [1,2,3])
        SEAT->>+REDIS: DEL hold:10:1, hold:10:2, hold:10:3
        REDIS-->>-SEAT: Deleted
        SEAT-->>-SVC: Holds released
        
        SVC-->>API: PaymentResponse(FAILED)
        API-->>GW: 200 OK
        GW-->>U: Payment failed
        Note right of U: Show error, seats released
    end
    end

    rect rgb(240, 220, 200)
    Note right of U: OPTIONAL: Verify Payment
    U->>+API: GET /api/payments/verify/100
    
    API->>+SVC: verifyPaymentStatus(100)
    
    Note over SVC: ⚠️ TODO: Query gateway API
    Note right of SVC: Should call gateway's<br/>transaction query endpoint<br/>to verify actual status
    
    SVC->>+DB: Load booking
    DB-->>-SVC: Booking(status:CONFIRMED)
    
    SVC-->>-API: "CONFIRMED"
    API-->>-U: 200 OK
    Note left of API: {status: "CONFIRMED"}
    end
```

### ⚠️ Critical Security Issues

1. **Signature Verification Missing**
```java
// MUST implement:
boolean verifySignature(PaymentRequest req) {
    String dataToSign = buildSignData(req); // All params except signature
    String calculatedHash = HmacSHA512(secretKey, dataToSign);
    return req.getSignature().equals(calculatedHash);
}
```

2. **Idempotency Check Missing**
```java
// MUST implement:
if (paymentTransactionRepository.existsByTransactionId(req.getTransactionId())) {
    log.warn("Duplicate transaction: {}", req.getTransactionId());
    return buildResponse(booking, "DUPLICATE");
}
```

3. **Amount Validation Missing**
```java
// MUST implement:
BigDecimal receivedAmount = new BigDecimal(req.getAmount());
if (receivedAmount.compareTo(booking.getTotalPrice()) != 0) {
    throw new BadRequestException("Amount mismatch");
}
```

---

## 7. Auto Booking Expiration

```mermaid
sequenceDiagram
    participant CRON as Cron Job
    participant SVC as BookingExpireService
    participant DB as MySQL
    participant SEAT as SeatDomainService
    participant REDIS as Redis

    Note over CRON,REDIS: AUTO BOOKING EXPIRATION (Every 5 minutes)

    rect rgb(200, 220, 240)
    Note right of CRON: @Scheduled(cron="0 */5 * * * *")
    
    CRON->>+SVC: expireBookings()
    
    Note over SVC: Find expired bookings
    SVC->>+DB: SELECT * FROM bookings
    Note right of SVC: WHERE status = 'PENDING_PAYMENT'<br/>AND booking_date < NOW() - INTERVAL 15 MINUTE
    
    alt No expired bookings
        DB-->>SVC: Empty list
        SVC->>SVC: log("No expired bookings")
        SVC-->>-CRON: Completed
        
    else Found expired bookings
        DB-->>-SVC: [Booking(id:100), Booking(id:101), ...]
        
        loop For each expired booking
            SVC->>+DB: BEGIN TRANSACTION
            
            SVC->>+DB: UPDATE bookings<br/>SET status='EXPIRED'<br/>WHERE id=100
            DB-->>-SVC: Updated
            
            SVC->>+DB: COMMIT
            DB-->>-SVC: Committed
            
            Note over SVC: Get seat IDs from booking
            SVC->>SVC: seatIds = booking.getBookingSeats()<br/>  .stream()<br/>  .map(bs -> bs.getSeat().getId())
            
            Note over SVC: Release Redis holds (if any)
            SVC->>+SEAT: releaseHolds(showtimeId, seatIds)
            
            loop For each seatId
                SEAT->>+REDIS: DEL hold:{showtimeId}:{seatId}
                REDIS-->>-SEAT: Deleted (or not exists)
            end
            
            SEAT-->>-SVC: Holds released
            
            SVC->>SVC: log("Booking {} expired, {} seats released", bookingId, seatIds.size())
        end
        
        SVC-->>-CRON: Completed
        Note right of CRON: Expired bookings processed<br/>Seats now available for others
    end
    end
```

### ⏰ Cron Expression
```java
@Scheduled(cron = "0 */5 * * * *")
// ┬ ┬  ┬ ┬ ┬ ┬
// │ │  │ │ │ └─ Day of week (all)
// │ │  │ │ └─── Month (all)
// │ │  │ └───── Day of month (all)
// │ │  └─────── Hour (all)
// │ └────────── Minute (every 5)
// └──────────── Second (0)
```

### 📊 Expiration Logic
```
Booking created at: 10:00:00
Payment deadline:   10:15:00  (15 minutes)
Cron runs at:       10:15:00  (every 5 min: 10:00, 10:05, 10:10, 10:15)
                    ↓
              Booking EXPIRED
                    ↓
            Seats RELEASED
```

---

## 8. Movie Search & Browse

```mermaid
sequenceDiagram
    participant U as User
    participant API as MovieController
    participant SVC as MovieService
    participant SEARCH as MovieSearchService
    participant DB as MySQL

    Note over U,DB: MOVIE SEARCH & BROWSE FLOW

    rect rgb(200, 220, 240)
    Note right of U: GET ALL MOVIES (Paginated)
    U->>+API: GET /api/movies?pageNumber=0&pageSize=12
    
    API->>+SVC: getAll(0, 12)
    
    SVC->>+DB: SELECT * FROM movies
    Note right of SVC: WHERE status != 'ENDED'<br/>ORDER BY release_date DESC<br/>LIMIT 12 OFFSET 0
    DB-->>-SVC: Page<Movie>
    
    SVC-->>-API: PageResponse
    API-->>-U: 200 OK
    Note left of API: {content: [movie1, movie2, ...],<br/>totalPages: 10,<br/>totalElements: 120}
    end

    rect rgb(220, 240, 200)
    Note right of U: SEARCH WITH FILTERS
    U->>+API: GET /api/movies/search
    Note right of U: ?keyword=spider<br/>&genres=ACTION<br/>&ratingMin=7.0<br/>&language=en<br/>&releaseFrom=2023-01-01<br/>&sortBy=rating<br/>&direction=DESC
    
    API->>+SEARCH: search(filter)
    
    Note over SEARCH: Build dynamic query (Specification API)
    SEARCH->>SEARCH: MovieSpecification.withFilters(filter)
    
    SEARCH->>+DB: SELECT * FROM movies
    Note right of SEARCH: WHERE (title LIKE '%spider%' OR description LIKE '%spider%')<br/>AND genre LIKE '%ACTION%'<br/>AND rating >= 7.0<br/>AND language = 'en'<br/>AND release_date >= '2023-01-01'<br/>ORDER BY rating DESC<br/>LIMIT 12
    
    DB-->>-SEARCH: Filtered results
    
    SEARCH-->>-API: PageResponse
    API-->>-U: 200 OK
    Note left of API: Filtered & sorted movies
    end

    rect rgb(240, 220, 200)
    Note right of U: GET MOVIE DETAIL
    U->>+API: GET /api/movies/1
    
    API->>+SVC: getById(1)
    
    SVC->>+DB: SELECT * FROM movies WHERE id = 1
    DB-->>-SVC: Movie object
    
    alt Movie found
        SVC-->>-API: MovieResponse
        API-->>-U: 200 OK
        Note left of API: {id, title, description,<br/>duration, rating, ...}
        
    else Movie not found
        SVC-->>API: NotFoundException
        API-->>U: 404 Not Found
        Note left of API: "Movie not found"
    end
    end
```

---

## 📊 Summary: Critical Flows

### 🔴 High Priority (Must Review)
1. **Booking Flow** - Concurrency control, locks, TOCTOU
2. **Payment Flow** - Security (signature verification)
3. **Authentication** - JWT validation, refresh token

### 🟡 Medium Priority
1. **Registration** - OTP rate limiting
2. **Auto-Expiration** - Cron job logic

### 🟢 Low Priority
1. **Movie Search** - Optimization, caching
2. **Forgot Password** - Edge cases

---

## 🎯 Next Steps

1. ✅ Review code với sequence diagrams này
2. 🐛 Đọc [ISSUES.md](./ISSUES.md) để fix bugs
3. 🧪 Test từng flow với Postman
4. 🔐 **Ưu tiên cao:** Implement payment gateway integration
5. 📧 Improve email templates & notifications

---

**📝 Note for Developers:**

- **Junior:** Focus trên simple flows (Login, Registration) trước
- **Middle:** Hiểu rõ Booking Flow và các cơ chế concurrency control
- **Senior:** Review toàn bộ, optimize performance, handle edge cases

**⚠️ Remember:** Sequence diagrams này phản ánh **current implementation**. Một số phần còn **TODO** và cần **implement**!
