# 🧪 Testing Guide - Movie Booking System

> Hướng dẫn chi tiết cách test từng API endpoint và flow nghiệp vụ

## 📋 Nội dung

1. [Setup môi trường test](#setup-môi-trường-test)
2. [Import Postman Collection](#import-postman-collection)
3. [Test Scenarios](#test-scenarios)
4. [End-to-End Testing](#end-to-end-testing)
5. [Load Testing](#load-testing)

---

## Setup môi trường test

### 1. Start services

```bash
# Start MySQL
docker run -d \
  --name mysql-test \
  -e MYSQL_ROOT_PASSWORD=123456789 \
  -e MYSQL_DATABASE=movie_booking \
  -p 3306:3306 \
  mysql:8.0

# Start Redis
docker run -d \
  --name redis-test \
  -p 6379:6379 \
  redis:6.0

# Start Spring Boot
mvn spring-boot:run
```

### 2. Verify services

```bash
# Check MySQL
mysql -h localhost -u root -p123456789 -e "SHOW DATABASES;"

# Check Redis
redis-cli ping

# Check API
curl http://localhost:8080/actuator/health
```

---

## Import Postman Collection

### 1. Import collection file
- File: `Movie_Booking_System_API_Tests.postman_collection.json`
- Postman → Import → Select file

### 2. Import environment
- File: `Movie_Booking_System_Environment.postman_environment.json`
- Chứa variables: `baseUrl`, `accessToken`, `refreshToken`

### 3. Run collection
- Click "Run Collection"
- Select environment
- Run all tests

---

## Test Scenarios

### 📝 Scenario 1: User Registration & Login

#### Step 1: Register account

```http
POST {{baseUrl}}/api/auth/register
Content-Type: application/json

{
  "username": "testuser001",
  "email": "test001@example.com",
  "password": "Test@1234",
  "firstName": "Test",
  "lastName": "User",
  "phoneNumber": "0912345678"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Success",
  "data": null
}
```

**Validation:**
- ✅ Status: 200 OK
- ✅ Check email inbox → OTP received
- ✅ Database: Account created với `email_verified=false`

#### Step 2: Activate account

```http
POST {{baseUrl}}/api/auth/activate
Content-Type: application/json

{
  "email": "test001@example.com",
  "otp": "123456"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Success"
}
```

**Validation:**
- ✅ Status: 200 OK
- ✅ Database: `email_verified=true`
- ✅ Redis: OTP deleted

#### Step 3: Login

```http
POST {{baseUrl}}/api/auth/login
Content-Type: application/json

{
  "username": "testuser001",
  "password": "Test@1234"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": "eyJhbGci..."
  }
}
```

**Validation:**
- ✅ Status: 200 OK
- ✅ accessToken is JWT (3 parts separated by `.`)
- ✅ Redis: refresh token stored
- **Save tokens to Postman environment:**
  ```javascript
  // In Tests tab
  pm.environment.set("accessToken", pm.response.json().data.accessToken);
  pm.environment.set("refreshToken", pm.response.json().data.refreshToken);
  ```

#### Step 4: Test authenticated request

```http
GET {{baseUrl}}/api/auth/me
Authorization: Bearer {{accessToken}}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Success"
}
```

---

### 🎬 Scenario 2: Browse Movies

#### Get all movies

```http
GET {{baseUrl}}/api/movies?pageNumber=0&pageSize=12
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Spider-Man: No Way Home",
        "description": "...",
        "duration": 148,
        "rating": 8.5,
        "genre": "ACTION",
        "language": "en",
        "status": "NOW_SHOWING"
      }
    ],
    "pageNumber": 0,
    "pageSize": 12,
    "totalPages": 5,
    "totalElements": 50
  }
}
```

#### Search movies

```http
GET {{baseUrl}}/api/movies/search?genres=ACTION&ratingMin=7.0&sortBy=rating&direction=DESC
```

#### Get movie detail

```http
GET {{baseUrl}}/api/movies/1
```

**Save movieId:**
```javascript
// Tests tab
pm.environment.set("movieId", pm.response.json().data.id);
```

---

### 🎟️ Scenario 3: Complete Booking Flow (CRITICAL)

#### Prerequisites:
- ✅ User logged in (có accessToken)
- ✅ Có movie, theater, screen, seats, showtime trong DB

#### Step 1: Get showtimes

```http
GET {{baseUrl}}/api/showtimes/by-theater-and-movie?theaterId=1&movieId=1&date=2024-12-25
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "movieTitle": "Spider-Man",
    "theaterName": "CGV Vincom",
    "showDate": "2024-12-25",
    "screenShowtimes": {
      "Screen 1": [
        {
          "showtimeId": 10,
          "startTime": "10:00",
          "endTime": "12:28",
          "price": 150000,
          "availableSeats": 48
        }
      ]
    }
  }
}
```

**Save showtimeId:**
```javascript
pm.environment.set("showtimeId", 10);
```

#### Step 2: Get seats for screen

```http
GET {{baseUrl}}/api/seats/screen/1
```

**Expected Response:**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "seatNumber": 1,
      "rowLabel": "A",
      "seatType": "STANDARD",
      "status": "AVAILABLE"
    },
    {
      "id": 2,
      "seatNumber": 2,
      "rowLabel": "A",
      "seatType": "VIP",
      "status": "AVAILABLE"
    }
  ]
}
```

**Select seats (save to environment):**
```javascript
pm.environment.set("seatIds", [1, 2, 3]);
```

#### Step 3: Hold seats (120 seconds)

```http
POST {{baseUrl}}/api/seats/hold
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "showtimeId": {{showtimeId}},
  "seatIds": [1, 2, 3],
  "ttlSec": 120
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Seats held successfully for 120 seconds. Please create booking before timeout."
}
```

**Validation:**
- ✅ Status: 200 OK
- ✅ Redis: Keys exist
  ```bash
  redis-cli
  > KEYS hold:10:*
  1) "hold:10:1"
  2) "hold:10:2"
  3) "hold:10:3"
  > TTL hold:10:1
  (integer) 118  # Còn 118 giây
  ```

**⏰ Important:** Bạn có **120 giây** để complete Step 4!

#### Step 4: Create booking (TRONG 120s!)

```http
POST {{baseUrl}}/api/bookings
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "showtimeId": {{showtimeId}},
  "seatIds": [1, 2, 3]
}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "Booking created successfully. Please complete payment within 15 minutes.",
  "data": {
    "id": 100,
    "totalPrice": 465000,
    "status": "PENDING_PAYMENT",
    "bookingDate": "2024-01-01T10:00:00",
    "seats": [
      {"seatId": 1, "price": 150000},
      {"seatId": 2, "price": 195000},
      {"seatId": 3, "price": 150000}
    ]
  }
}
```

**Validation:**
- ✅ Status: 200 OK
- ✅ Database: Booking created
  ```sql
  SELECT * FROM bookings WHERE id = 100;
  -- status = 'PENDING_PAYMENT'
  
  SELECT * FROM booking_seats WHERE booking_id = 100;
  -- 3 rows
  ```
- ✅ Redis: Holds consumed (deleted)
  ```bash
  redis-cli
  > KEYS hold:10:*
  (empty array)  # Holds đã bị consumed
  ```

**Save bookingId:**
```javascript
pm.environment.set("bookingId", pm.response.json().data.id);
```

**⏰ Important:** Bạn có **15 phút** để payment!

#### Step 5: Create payment URL

```http
POST {{baseUrl}}/api/payments/create/{{bookingId}}
Authorization: Bearer {{accessToken}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": "https://payment-gateway.example.com/checkout?bookingId=100&amount=465000",
  "message": "Redirect to payment gateway. Complete payment within 15 minutes."
}
```

**⚠️ Note:** Đây là MOCK URL. Trong thực tế sẽ là VNPay/MoMo URL.

#### Step 6: Simulate payment callback (Mock)

```http
POST {{baseUrl}}/api/payments/callback
Content-Type: application/json

{
  "bookingId": {{bookingId}},
  "status": "SUCCESS",
  "transactionId": "TXN123456",
  "amount": "465000",
  "signature": "mock-signature"
}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "bookingId": 100,
    "status": "SUCCESS",
    "message": "Payment completed successfully"
  }
}
```

**Validation:**
- ✅ Database: Booking status = CONFIRMED
  ```sql
  SELECT status FROM bookings WHERE id = 100;
  -- CONFIRMED
  ```
- ✅ Email sent (check inbox)

#### Step 7: Verify booking

```http
GET {{baseUrl}}/api/bookings/{{bookingId}}
Authorization: Bearer {{accessToken}}
```

**Expected Response:**
```json
{
  "code": 200,
  "data": {
    "id": 100,
    "status": "CONFIRMED",
    "totalPrice": 465000
  }
}
```

**✅ Booking flow completed successfully!**

---

### 🔴 Scenario 4: Error Cases & Race Conditions

#### Test 1: Concurrent seat hold

**Setup:** 2 users cùng hold 1 ghế

```http
# User A (với token A)
POST {{baseUrl}}/api/seats/hold
Authorization: Bearer {{accessTokenA}}
{
  "showtimeId": 10,
  "seatIds": [5]
}
# Expected: 200 OK

# User B (với token B) - cùng lúc
POST {{baseUrl}}/api/seats/hold
Authorization: Bearer {{accessTokenB}}
{
  "showtimeId": 10,
  "seatIds": [5]
}
# Expected: 409 Conflict
# Message: "Seat 5 is held by another user"
```

#### Test 2: Hold timeout

```http
# 1. Hold ghế với TTL ngắn
POST /api/seats/hold
{
  "showtimeId": 10,
  "seatIds": [6],
  "ttlSec": 5
}

# 2. Đợi 6 giây...

# 3. Try to create booking
POST /api/bookings
{
  "showtimeId": 10,
  "seatIds": [6]
}

# Expected: 409 Conflict
# Message: "Seat 6 no longer held (expired)"
```

#### Test 3: Payment timeout (auto-expiration)

```http
# 1. Create booking
POST /api/bookings
# Response: bookingId=200, status=PENDING_PAYMENT

# 2. Không thanh toán

# 3. Đợi > 15 phút (hoặc trigger cron manual)

# 4. Check booking
GET /api/bookings/200
# Expected: status=EXPIRED

# 5. Try to hold lại ghế
POST /api/seats/hold
{
  "seatIds": [1, 2, 3]  # Ghế từ booking 200
}
# Expected: 200 OK (ghế đã available)
```

#### Test 4: Invalid OTP

```http
POST /api/auth/activate
{
  "email": "test@example.com",
  "otp": "999999"  # Wrong OTP
}

# Expected: 400 Bad Request
# Message: "Invalid or expired OTP"
```

#### Test 5: Expired JWT token

```http
# Wait for access token to expire (30 min)

GET /api/bookings
Authorization: Bearer {expired-token}

# Expected: 401 Unauthorized
```

---

## End-to-End Testing

### E2E Scenario: Complete user journey

```
1. Register → Activate → Login
2. Browse movies → Search
3. Select movie → Get showtimes
4. Hold seats (120s)
5. Create booking (15 min)
6. Payment → Confirmation
7. Receive email with QR code
8. Logout
```

### Automation script (Postman)

```javascript
// Collection Runner script
pm.test("E2E: Complete booking flow", function() {
    // 1. Register
    pm.sendRequest({
        url: pm.environment.get("baseUrl") + "/api/auth/register",
        method: "POST",
        header: {"Content-Type": "application/json"},
        body: {
            mode: "raw",
            raw: JSON.stringify({
                username: "e2e_user_" + Date.now(),
                email: "e2e_" + Date.now() + "@example.com",
                password: "Test@1234",
                firstName: "E2E",
                lastName: "Test",
                phoneNumber: "0912345678"
            })
        }
    }, function(err, res) {
        pm.expect(res.code).to.equal(200);
        
        // Get OTP from email...
        // Activate...
        // Login...
        // Complete booking...
    });
});
```

---

## Load Testing

### Apache JMeter Test Plan

```xml
<!-- booking-load-test.jmx -->
<jmeterTestPlan>
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">Concurrent Booking Test</stringProp>
      <elementProp name="ThreadGroup">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">60</stringProp>
      </elementProp>
      
      <!-- HTTP Request: Hold Seats -->
      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.path">/api/seats/hold</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
      </HTTPSamplerProxy>
      
      <!-- HTTP Request: Create Booking -->
      <HTTPSamplerProxy>
        <stringProp name="HTTPSampler.path">/api/bookings</stringProp>
        <stringProp name="HTTPSampler.method">POST</stringProp>
      </HTTPSamplerProxy>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

### Run load test

```bash
jmeter -n -t booking-load-test.jmx -l results.jtl

# Expected results:
# - 100 concurrent users
# - 0% error rate
# - Average response time < 1s
# - No deadlocks
# - No duplicate bookings
```

### Monitor Redis during load test

```bash
redis-cli --stat

# Watch for:
# - Connection count
# - Command rate
# - Key count (holds, locks)
```

---

## 🎯 Test Checklist

### Before deployment

#### Functional Tests
- [ ] ✅ Registration flow works
- [ ] ✅ Login/Logout works
- [ ] ✅ JWT refresh works
- [ ] ✅ OTP flow works
- [ ] ✅ Password reset works
- [ ] ✅ Movie CRUD works
- [ ] ✅ Showtime CRUD works
- [ ] ✅ Seat hold works
- [ ] ✅ Booking creation works
- [ ] ✅ Payment flow works
- [ ] ✅ Auto-expiration works

#### Error Handling
- [ ] ✅ Invalid input handled
- [ ] ✅ 404 for not found
- [ ] ✅ 401 for unauthorized
- [ ] ✅ 409 for conflicts
- [ ] ✅ 500 errors logged properly

#### Security Tests
- [ ] ✅ SQL injection prevention
- [ ] ✅ XSS prevention
- [ ] ✅ JWT validation works
- [ ] ✅ Password hashing works
- [ ] ⚠️ Payment signature verification (TODO)

#### Performance Tests
- [ ] ✅ Concurrent booking (100 users)
- [ ] ✅ No deadlocks
- [ ] ✅ No duplicate bookings
- [ ] ✅ Redis connection pool stable

#### Integration Tests
- [ ] ✅ MySQL connection stable
- [ ] ✅ Redis connection stable
- [ ] ✅ Email sending works
- [ ] ⚠️ Payment gateway (TODO)

---

## 📝 Test Report Template

```markdown
# Test Report - [Date]

## Summary
- Total Tests: 50
- Passed: 45
- Failed: 5
- Skipped: 0

## Failed Tests
1. **Payment signature verification**
   - Status: FAILED
   - Reason: Not implemented
   - Priority: CRITICAL
   
2. **Email QR code**
   - Status: FAILED
   - Reason: QR code generation not implemented
   - Priority: MAJOR

## Performance Metrics
- Average response time: 250ms
- 95th percentile: 800ms
- Error rate: 0.5%

## Recommendations
1. Implement payment signature verification
2. Add QR code generation
3. Improve error messages
```

---

## 🚀 Next Steps

1. ✅ Run all tests locally
2. ✅ Fix failed tests
3. ✅ Run load tests
4. ✅ Deploy to staging
5. ✅ Run tests on staging
6. ✅ Deploy to production

---

**Happy Testing!** 🎉
