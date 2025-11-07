# 🐛 Issues & Improvements - Movie Booking System

> Danh sách đầy đủ các vấn đề, bugs và improvements cần thực hiện

## 📋 Tổng quan

| Priority | Category | Count | Status |
|----------|----------|-------|--------|
| 🔴 CRITICAL | Security, Payment | 5 | ⚠️ **Urgent** |
| 🟡 MAJOR | Performance, Logic | 8 | 📌 Important |
| 🟢 MINOR | Code Quality, UX | 12 | 💡 Nice to have |

**Total Issues: 25**

---

## 🔴 CRITICAL Issues (Must Fix Immediately)

### 1. ⚠️ **Payment Gateway chưa được integrate**

**Severity:** 🔴 CRITICAL  
**Impact:** System không thể accept thanh toán thật → Không production-ready  
**Location:** `PaymentServiceImpl.java`

**Current State:**
```java
// MOCK URL - Not production ready!
String mockPaymentUrl = "https://payment-gateway.example.com/checkout?bookingId=" + bookingId;
```

**Issue:**
- Payment URL là hardcoded mock
- Không có integration với VNPay/MoMo/Stripe
- User không thể thanh toán thật

**Fix Required:**
```java
// 1. Choose gateway: VNPay (Vietnam) or Stripe (International)
// 2. Add dependencies:
<dependency>
    <groupId>com.stripe</groupId>
    <artifactId>stripe-java</artifactId>
    <version>24.1.0</version>
</dependency>

// 3. Implement real payment URL generation:
@Value("${payment.vnpay.tmnCode}")
private String tmnCode;

@Value("${payment.vnpay.hashSecret}")
private String hashSecret;

public String createPaymentUrl(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)...
    
    Map<String, String> params = new TreeMap<>();
    params.put("vnp_Version", "2.1.0");
    params.put("vnp_Command", "pay");
    params.put("vnp_TmnCode", tmnCode);
    params.put("vnp_Amount", String.valueOf(
        booking.getTotalPrice().multiply(BigDecimal.valueOf(100)).longValue()
    )); // VNPay yêu cầu amount * 100
    params.put("vnp_CurrencyCode", "VND");
    params.put("vnp_TxnRef", String.valueOf(bookingId));
    params.put("vnp_OrderInfo", "Thanh toan ve xem phim " + bookingId);
    params.put("vnp_ReturnUrl", returnUrl);
    params.put("vnp_IpAddr", getClientIp());
    params.put("vnp_CreateDate", formatDate(LocalDateTime.now()));
    
    // Generate secure hash
    String signData = buildQueryString(params);
    String secureHash = HmacSHA512(hashSecret, signData);
    params.put("vnp_SecureHash", secureHash);
    
    return vnpUrl + "?" + buildQueryString(params);
}
```

**Testing:**
- Use VNPay sandbox: https://sandbox.vnpayment.vn
- Test credentials từ VNPay documentation

**References:**
- VNPay Docs: https://sandbox.vnpayment.vn/apis/docs/
- MoMo Docs: https://developers.momo.vn/
- Stripe Docs: https://stripe.com/docs/payments

**Estimated Effort:** 2-3 days

---

### 2. 🚨 **Payment Callback không verify signature (CRITICAL SECURITY FLAW!)**

**Severity:** 🔴 CRITICAL  
**Impact:** Attacker có thể fake payment success → Đặt vé miễn phí!  
**Location:** `PaymentServiceImpl.handlePaymentCallback()`

**Current State:**
```java
// TODO: CRITICAL - VERIFY SIGNATURE
// if (!verifyPaymentSignature(request)) {
//     throw new SecurityException("Invalid payment signature");
// }
```

**Issue:**
- Không verify signature từ payment gateway
- Attacker có thể POST fake callback:
```bash
curl -X POST http://localhost:8080/api/payments/callback \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": 123,
    "status": "SUCCESS",
    "transactionId": "FAKE123",
    "amount": "500000"
  }'
```
→ Booking sẽ được confirm mà không thanh toán!

**Fix Required:**
```java
@Override
@Transactional
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // STEP 1: VERIFY SIGNATURE (CRITICAL!)
    if (!verifyPaymentSignature(request)) {
        log.error("⚠️ SECURITY ALERT: Invalid payment signature for booking {}", 
            request.getBookingId());
        throw new SecurityException("Invalid payment signature");
    }
    
    // STEP 2: Check idempotency
    if (paymentTransactionRepository.existsByTransactionId(request.getTransactionId())) {
        log.warn("Duplicate transaction: {}", request.getTransactionId());
        return buildDuplicateResponse();
    }
    
    // STEP 3: Validate amount
    Booking booking = bookingRepository.findById(request.getBookingId())...
    BigDecimal receivedAmount = new BigDecimal(request.getAmount());
    if (receivedAmount.compareTo(booking.getTotalPrice()) != 0) {
        log.error("Amount mismatch! Expected: {}, Received: {}", 
            booking.getTotalPrice(), receivedAmount);
        throw new BadRequestException("Payment amount mismatch");
    }
    
    // Now safe to process...
}

private boolean verifyPaymentSignature(PaymentRequest request) {
    // For VNPay:
    Map<String, String> params = new TreeMap<>();
    params.put("vnp_TxnRef", request.getBookingId().toString());
    params.put("vnp_Amount", request.getAmount());
    params.put("vnp_ResponseCode", request.getStatus());
    // ... add all params except vnp_SecureHash
    
    String signData = buildSignData(params);
    String calculatedHash = HmacSHA512(hashSecret, signData);
    
    return calculatedHash.equals(request.getSignature());
}
```

**Testing:**
```bash
# Attack scenario (before fix):
curl -X POST http://localhost:8080/api/payments/callback \
  -d '{"bookingId":1,"status":"SUCCESS","signature":"FAKE"}'
Expected: 403 Forbidden, "Invalid signature"

# After fix, must use valid signature from gateway
```

**Estimated Effort:** 1 day

---

### 3. ⚠️ **Thiếu Webhook endpoint cho async payment notifications**

**Severity:** 🔴 CRITICAL  
**Impact:** Payment có thể success nhưng không update booking (nếu user đóng browser)  
**Location:** `PaymentController` - endpoint missing

**Issue:**
- Chỉ dựa vào callback URL (user redirect)
- Nếu user đóng browser sau khi thanh toán, callback không được gọi
- Payment gateway cũng gửi webhook (async), nhưng không có endpoint

**Fix Required:**
```java
@PostMapping("/webhook")
public ResponseEntity<?> handleWebhook(
    @RequestBody String payload,
    @RequestHeader("X-VNPay-Signature") String signature
) {
    log.info("[PAYMENT-WEBHOOK] Received webhook");
    
    // 1. Verify webhook signature (different from callback!)
    if (!verifyWebhookSignature(payload, signature)) {
        log.error("⚠️ SECURITY: Invalid webhook signature");
        return ResponseEntity.status(401).body("Invalid signature");
    }
    
    // 2. Parse payload
    PaymentWebhook webhook = parseWebhook(payload);
    
    // 3. Process idempotently
    try {
        paymentService.processWebhook(webhook);
        return ResponseEntity.ok("OK");
    } catch (Exception e) {
        log.error("Webhook processing failed", e);
        return ResponseEntity.status(500).body("Error");
    }
}
```

**Configuration:**
```yaml
# application.yml
payment:
  vnpay:
    webhookSecret: your-webhook-secret  # Different from payment secret!
```

**Estimated Effort:** 1 day

---

### 4. 🚨 **Không có idempotency check cho payment**

**Severity:** 🔴 CRITICAL  
**Impact:** Double payment processing → User bị charge 2 lần  
**Location:** `PaymentServiceImpl.handlePaymentCallback()`

**Issue:**
```java
// Current code: Không check duplicate transaction
if ("SUCCESS".equals(request.getStatus())) {
    booking.setStatus(BookingStatus.CONFIRMED);  // Xử lý lại nếu callback gọi 2 lần!
}
```

**Scenario:**
1. Payment gateway gọi callback lần 1 → Success
2. Network timeout → Gateway retry
3. Callback gọi lần 2 với cùng transactionId
4. Booking confirm 2 lần? Email gửi 2 lần?

**Fix Required:**
```java
// 1. Create Payment entity
@Entity
public class Payment {
    @Id
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String transactionId;  // From gateway
    
    @ManyToOne
    private Booking booking;
    
    private BigDecimal amount;
    private String status;
    private LocalDateTime processedAt;
}

// 2. Check idempotency
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // Check if already processed
    Optional<Payment> existing = paymentRepository
        .findByTransactionId(request.getTransactionId());
    
    if (existing.isPresent()) {
        log.warn("Duplicate payment callback: {}", request.getTransactionId());
        return PaymentResponse.builder()
            .status("DUPLICATE")
            .message("Transaction already processed")
            .build();
    }
    
    // Process payment...
    Payment payment = Payment.builder()
        .transactionId(request.getTransactionId())
        .booking(booking)
        .amount(receivedAmount)
        .status("SUCCESS")
        .processedAt(LocalDateTime.now())
        .build();
    
    paymentRepository.save(payment);  // Unique constraint ensures idempotency
    // ...
}
```

**Estimated Effort:** 4 hours

---

### 5. ⚠️ **Access token vẫn valid sau logout (Token Blacklisting missing)**

**Severity:** 🔴 CRITICAL  
**Impact:** Security risk - stolen token vẫn dùng được sau logout  
**Location:** `AuthServiceImpl.logout()`

**Current State:**
```java
public void logout(String refreshToken) {
    // Chỉ xóa refresh token trong Redis
    redisService.delete(redisKey);
    // Access token vẫn valid cho đến khi hết hạn (30 phút)!
}
```

**Issue:**
```bash
# 1. User login
POST /api/auth/login
Response: {accessToken: "abc...", refreshToken: "xyz..."}

# 2. Attacker steal accessToken

# 3. User logout
POST /api/auth/logout

# 4. Attacker vẫn dùng được accessToken (trong 30 phút)!
GET /api/bookings
Header: Authorization: Bearer abc...
Response: 200 OK (SHOULD BE 401!)
```

**Fix Required:**

**Option 1: Token Blacklist (Recommended)**
```java
// 1. Add to Redis on logout
public void logout(String refreshToken) {
    String username = jwtProvider.extractUsername(refreshToken);
    
    // Blacklist refresh token
    redisService.delete("auth:refreshToken:" + username);
    
    // Blacklist access token (if provided)
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null) {
        String accessToken = extractTokenFromRequest();
        long ttl = jwtProvider.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        
        // Store in blacklist until expiration
        redisService.set(
            "auth:blacklist:" + accessToken,
            "revoked",
            ttl,
            TimeUnit.MILLISECONDS
        );
    }
}

// 2. Check blacklist in JwtFilter
public class JwtFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String token = extractToken(request);
        
        // Check blacklist
        if (redisService.hasKey("auth:blacklist:" + token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        
        // Continue validation...
    }
}
```

**Option 2: Short-lived Access Token + Rotation**
```yaml
jwt:
  access-token-expiry-minutes: 5  # Reduce from 30 to 5 minutes
  refresh-token-expiry-days: 1
```

**Estimated Effort:** 4 hours

---

## 🟡 MAJOR Issues (Should Fix Soon)

### 6. ⚠️ **Pagination chưa implement đầy đủ**

**Severity:** 🟡 MAJOR  
**Impact:** API không hoạt động, throw UnsupportedOperationException  
**Location:** `BookingServiceImpl.getAlls()`

**Current State:**
```java
@Override
public PageResponse<?> getAlls(int pageNumber, int pageSize) {
    // TODO: Implement pagination with proper sorting
    throw new UnsupportedOperationException("Pagination not yet implemented");
}
```

**Fix Required:**
```java
@Override
public PageResponse<?> getAlls(int pageNumber, int pageSize) {
    Pageable pageable = PageRequest.of(
        pageNumber, 
        pageSize, 
        Sort.by("bookingDate").descending()
    );
    
    Page<Booking> page = bookingRepository.findAll(pageable);
    
    List<BookingResponse> content = page.getContent().stream()
        .map(BookingMapper::toResponse)
        .toList();
    
    return PageResponse.builder()
        .pageNumber(page.getNumber())
        .pageSize(page.getSize())
        .totalPages(page.getTotalPages())
        .totalElements(page.getTotalElements())
        .content(content)
        .build();
}

// Add filters
public PageResponse<?> getAlls(int pageNumber, int pageSize, BookingFilter filter) {
    Specification<Booking> spec = BookingSpecification.withFilters(filter);
    Page<Booking> page = bookingRepository.findAll(spec, pageable);
    // ...
}
```

**Add Filters:**
```java
public class BookingFilter {
    private Long userId;
    private BookingStatus status;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private Long showtimeId;
}
```

**Estimated Effort:** 4 hours

---

### 7. ⚠️ **Booking update/delete endpoints không nên tồn tại**

**Severity:** 🟡 MAJOR  
**Impact:** Confusing API, endpoints throw exception  
**Location:** `BookingController.update()`, `BookingController.delete()`

**Current State:**
```java
@PatchMapping("/{bookingId}")
public ResponseEntity<?> update(...) {
    // Throws: UnsupportedOperationException
    return ResponseEntity.ok(bookingService.update(bookingId, request));
}

@DeleteMapping("/{bookingId}")
public ResponseEntity<?> delete(...) {
    // Throws: UnsupportedOperationException
    bookingService.delete(bookingId);
    return ResponseEntity.ok(BaseResponse.success());
}
```

**Issue:**
- Endpoints tồn tại nhưng throw exception
- Không follow REST principles properly
- Confusing cho API users

**Fix Required:**

**Option 1: Remove endpoints (Recommended)**
```java
// BookingController.java
// ❌ Remove these methods entirely

// Keep only:
@PostMapping
public ResponseEntity<?> create(...)

@GetMapping("/{bookingId}")
public ResponseEntity<?> getById(...)

@GetMapping
public ResponseEntity<?> getAlls(...)
```

**Option 2: Implement proper cancellation**
```java
@PostMapping("/{bookingId}/cancel")
public ResponseEntity<?> cancel(@PathVariable Long bookingId) {
    bookingService.cancel(bookingId);
    return ResponseEntity.ok("Booking cancelled");
}

// Service implementation
public void cancel(Long bookingId) {
    Booking booking = bookingRepository.findById(bookingId)...
    
    // Only allow cancel if PENDING_PAYMENT
    if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
        throw new BadRequestException("Can only cancel pending bookings");
    }
    
    booking.setStatus(BookingStatus.CANCELLED);
    bookingRepository.save(booking);
    
    // Release seats
    List<Long> seatIds = booking.getBookingSeats().stream()...
    seatDomainService.releaseHolds(booking.getShowtime().getId(), seatIds);
}
```

**Estimated Effort:** 2 hours

---

### 8. ⚠️ **Redis connection pool chưa được configure**

**Severity:** 🟡 MAJOR  
**Impact:** Performance bottleneck khi high traffic  
**Location:** `application.yml`

**Current State:**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # Không có connection pool config!
```

**Issue:**
- Sử dụng default connection pool settings
- Default pool size = 8 (quá nhỏ cho production)
- Không có timeout configuration
- Risk: Connection exhaustion khi high traffic

**Fix Required:**
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms  # Connection timeout
      
      # Lettuce connection pool
      lettuce:
        pool:
          max-active: 20      # Max connections
          max-idle: 10        # Max idle connections
          min-idle: 5         # Min idle connections
          max-wait: 2000ms    # Max wait time
        shutdown-timeout: 200ms
        
      # Optional: SSL for production
      ssl: false
      
      # Optional: Password
      # password: your-redis-password
```

**For High Traffic:**
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 50      # Increase for production
          max-idle: 20
          min-idle: 10
```

**Testing:**
```java
@Test
public void testRedisPoolUnderLoad() {
    ExecutorService executor = Executors.newFixedThreadPool(100);
    
    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> {
            redisService.set("test:" + UUID.randomUUID(), "value");
        });
    }
    
    executor.shutdown();
    executor.awaitTermination(10, TimeUnit.SECONDS);
    
    // Should not throw PoolExhaustedException
}
```

**Estimated Effort:** 1 hour

---

### 9. ⚠️ **Email templates quá đơn giản, thiếu booking confirmation**

**Severity:** 🟡 MAJOR  
**Impact:** Poor UX, không có email sau payment success  
**Location:** `MailServiceImpl`, `PaymentServiceImpl`

**Current State:**
```java
// Only OTP emails implemented
// No booking confirmation email!

public PaymentResponse handlePaymentCallback(...) {
    booking.setStatus(BookingStatus.CONFIRMED);
    bookingRepository.save(booking);
    
    // TODO: Send confirmation email (NOT IMPLEMENTED!)
}
```

**Fix Required:**

**1. Create email templates:**

`src/main/resources/templates/booking-confirmation.html`
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <style>
        body { font-family: Arial, sans-serif; }
        .container { max-width: 600px; margin: 0 auto; }
        .header { background: #4CAF50; color: white; padding: 20px; }
        .qr-code { text-align: center; padding: 20px; }
        .details { padding: 20px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🎬 Booking Confirmed!</h1>
        </div>
        
        <div class="details">
            <p>Hi <span th:text="${userName}">User</span>,</p>
            <p>Your booking has been confirmed!</p>
            
            <h3>Booking Details:</h3>
            <ul>
                <li><strong>Booking ID:</strong> <span th:text="${bookingId}"></span></li>
                <li><strong>Movie:</strong> <span th:text="${movieTitle}"></span></li>
                <li><strong>Theater:</strong> <span th:text="${theaterName}"></span></li>
                <li><strong>Screen:</strong> <span th:text="${screenName}"></span></li>
                <li><strong>Date:</strong> <span th:text="${showDate}"></span></li>
                <li><strong>Time:</strong> <span th:text="${showTime}"></span></li>
                <li><strong>Seats:</strong> <span th:text="${seats}"></span></li>
                <li><strong>Total:</strong> <span th:text="${totalPrice}"></span> VND</li>
            </ul>
        </div>
        
        <div class="qr-code">
            <img th:src="'data:image/png;base64,' + ${qrCode}" alt="QR Code"/>
            <p>Show this QR code at the cinema</p>
        </div>
    </div>
</body>
</html>
```

**2. Generate QR Code:**
```java
// Add dependency
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.1</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.1</version>
</dependency>

// Service
@Service
public class QRCodeService {
    public String generateBookingQR(Long bookingId) throws WriterException, IOException {
        String qrContent = "BOOKING:" + bookingId;
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrContent, 
            BarcodeFormat.QR_CODE, 
            300, 300
        );
        
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        
        return Base64.getEncoder().encodeToString(pngData);
    }
}
```

**3. Send email:**
```java
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // ... update booking status ...
    
    // Send confirmation email
    String qrCode = qrCodeService.generateBookingQR(booking.getId());
    
    Map<String, Object> variables = new HashMap<>();
    variables.put("userName", booking.getAccount().getUser().getFirstName());
    variables.put("bookingId", booking.getId());
    variables.put("movieTitle", booking.getShowtime().getMovie().getTitle());
    // ... other details ...
    variables.put("qrCode", qrCode);
    
    emailService.sendTemplateEmail(
        booking.getAccount().getEmail(),
        "🎬 Booking Confirmed - " + booking.getShowtime().getMovie().getTitle(),
        "booking-confirmation",
        variables
    );
}
```

**Estimated Effort:** 1 day

---

### 10. ⚠️ **Thiếu transaction management một số nơi**

**Severity:** 🟡 MAJOR  
**Impact:** Risk data inconsistency khi có exception  
**Location:** Multiple service classes

**Issues:**

**1. SeatService.generateSeats() không có @Transactional**
```java
// Current
public List<SeatResponse> generateSeats(Long screenId, SeatGenerationRequest request) {
    // Creates multiple seats
    // If fail ở giữa → inconsistent data!
}

// Fix
@Transactional
public List<SeatResponse> generateSeats(Long screenId, SeatGenerationRequest request) {
    // Now atomic
}
```

**2. ShowtimeService.create() không có @Transactional**
```java
// Fix
@Transactional
public ShowtimeResponse create(ShowtimeRequest request) {
    // Ensures atomic creation
}
```

**3. MovieService operations**
```java
@Transactional
public MovieResponse create(MovieRequest request) {
    // Should be transactional
}

@Transactional
public MovieResponse update(Long id, UpdateMovieRequest request) {
    // Should be transactional
}
```

**Best Practice:**
```java
@Service
@Transactional(readOnly = true)  // Default for all methods
public class BookingServiceImpl implements BookingService {
    
    @Transactional  // Override for write operations
    public BookingResponse create(BookingRequest request) {
        // ...
    }
    
    // Read methods inherit readOnly=true
    public BookingResponse getById(Long id) {
        // ...
    }
}
```

**Estimated Effort:** 2 hours

---

### 11. ⚠️ **Soft delete không consistent**

**Severity:** 🟡 MAJOR  
**Impact:** Data management complexity  
**Location:** Multiple entities

**Current State:**
```java
// Seat.java - Has soft delete
@SQLDelete(sql = "UPDATE seats SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Seat {
    private boolean isDeleted = false;
}

// Movie.java - No soft delete (hard delete)
public class Movie {
    // Just normal entity
}

// Theater.java - No soft delete
// Screen.java - No soft delete
```

**Issue:**
- Inconsistent deletion strategy
- Seat có soft delete nhưng các entity khác không
- Khi delete Theater → cascade delete Seats → Seats bị hard delete?

**Fix Required:**

**Option 1: Implement soft delete cho tất cả (Recommended)**
```java
// BaseEntity.java
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Add soft delete fields
    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

// Global config
@Configuration
public class HibernateConfig {
    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return new CamelCaseToUnderscoresNamingStrategy();
    }
}

// All entities
@Entity
@SQLDelete(sql = "UPDATE table_name SET is_deleted = true, deleted_at = NOW() WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Movie extends BaseEntity {
    // ...
}
```

**Option 2: Remove soft delete (không recommended cho production)**
```java
// Remove @SQLDelete và @Where từ Seat.java
// Use hard delete everywhere
```

**Estimated Effort:** 4 hours

---

### 12. ⚠️ **Thiếu rate limiting cho APIs**

**Severity:** 🟡 MAJOR  
**Impact:** Vulnerable to DoS attacks  
**Location:** All controllers

**Current State:**
```java
// Chỉ có OTP rate limit
// Các API khác không có rate limiting!

POST /api/auth/register  // No rate limit
POST /api/bookings       // No rate limit
GET /api/movies          // No rate limit
```

**Issue:**
- Attacker có thể spam requests → DoS
- No protection against brute force

**Fix Required:**

**Use Bucket4j + Redis:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.5.0</version>
</dependency>
```

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, ...) {
        String clientId = getClientId(request);  // IP or userId
        String key = "ratelimit:" + clientId;
        
        // Allow 100 requests per minute
        long count = redis.opsForValue().increment(key);
        
        if (count == 1) {
            redis.expire(key, 1, TimeUnit.MINUTES);
        }
        
        if (count > 100) {
            response.setStatus(429);  // Too Many Requests
            response.getWriter().write("Rate limit exceeded");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientId(HttpServletRequest request) {
        // Try to get userId from JWT
        String token = extractToken(request);
        if (token != null) {
            return jwtProvider.extractUsername(token);
        }
        
        // Fallback to IP
        return request.getRemoteAddr();
    }
}
```

**Per-endpoint limits:**
```java
@Configuration
public class RateLimitConfig {
    public static final Map<String, Integer> ENDPOINT_LIMITS = Map.of(
        "POST:/api/auth/register", 5,      // 5/min
        "POST:/api/auth/login", 10,         // 10/min
        "POST:/api/bookings", 20,           // 20/min
        "GET:/api/movies", 100              // 100/min
    );
}
```

**Estimated Effort:** 1 day

---

### 13. ⚠️ **Lock timeout hardcoded, không configurable**

**Severity:** 🟡 MAJOR  
**Impact:** Khó điều chỉnh performance  
**Location:** `BookingServiceImpl.java`

**Current State:**
```java
if (!redisLockService.tryLockSeat(showtimeId, seatId, 30, TimeUnit.SECONDS)) {
    // Hardcoded timeout: 30 seconds
}
```

**Fix Required:**
```yaml
# application.yml
booking:
  lock-timeout-seconds: 30
  seat-hold-ttl-seconds: 120
  payment-timeout-minutes: 15
```

```java
@Service
public class BookingServiceImpl {
    @Value("${booking.lock-timeout-seconds}")
    private int lockTimeoutSeconds;
    
    @Value("${booking.seat-hold-ttl-seconds}")
    private int seatHoldTtlSeconds;
    
    public BookingResponse create(...) {
        // Use configurable timeout
        if (!redisLockService.tryLockSeat(
            showtimeId, seatId, 
            lockTimeoutSeconds, TimeUnit.SECONDS
        )) {
            // ...
        }
    }
}
```

**Estimated Effort:** 1 hour

---

## 🟢 MINOR Issues (Nice to Have)

### 14. 💡 **Logging chưa structured, thiếu correlation ID**

**Severity:** 🟢 MINOR  
**Impact:** Khó debug distributed systems  
**Location:** All services

**Current State:**
```java
log.info("[BOOKING] Create booking request: {}", request);
// Không có correlation ID để trace request qua nhiều services
```

**Fix Required:**
```java
// 1. Add correlation ID filter
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        MDC.put("correlationId", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}

// 2. Update logback pattern
<pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %highlight(%-5level) [%X{correlationId}] %cyan(%logger{36}) - %msg%n</pattern>

// 3. Logs will look like:
// 2024-01-01 10:00:00 [http-nio-8080-exec-1] INFO [abc-123-def-456] BookingService - Create booking
// 2024-01-01 10:00:01 [http-nio-8080-exec-1] INFO [abc-123-def-456] PaymentService - Create payment
```

**Estimated Effort:** 2 hours

---

### 15. 💡 **Error messages hardcoded (không i18n)**

**Severity:** 🟢 MINOR  
**Impact:** Khó internationalize  

**Fix:**
```properties
# messages.properties
error.booking.not.found=Booking not found with ID: {0}
error.seat.already.held=Seat {0} is held by another user

# messages_vi.properties
error.booking.not.found=Không tìm thấy booking với ID: {0}
error.seat.already.held=Ghế {0} đang được giữ bởi người khác
```

```java
@Service
public class MessageService {
    @Autowired
    private MessageSource messageSource;
    
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
```

**Estimated Effort:** 4 hours

---

### 16-25. Other Minor Issues

- **16.** Thiếu API documentation examples trong Swagger
- **17.** Không có health check cho Redis & MySQL
- **18.** Unit test coverage thấp
- **19.** Không có API versioning (/api/v1/...)
- **20.** Response time metrics không được track
- **21.** Database indexes chưa được optimize đầy đủ
- **22.** Không có request/response logging cho audit
- **23.** Exception messages expose quá nhiều thông tin
- **24.** Không có HTTPS enforcement
- **25.** Thiếu CSRF protection (stateless API nên không cần?)

---

## 📊 Fix Priority Roadmap

### Sprint 1 (Week 1-2): Critical Security
- [ ] Fix #2: Payment signature verification
- [ ] Fix #1: Payment gateway integration
- [ ] Fix #3: Webhook endpoint
- [ ] Fix #4: Idempotency check
- [ ] Fix #5: Token blacklisting

### Sprint 2 (Week 3): Major Features
- [ ] Fix #6: Implement pagination
- [ ] Fix #7: Remove/fix booking update/delete
- [ ] Fix #8: Redis connection pool
- [ ] Fix #9: Email templates & QR code

### Sprint 3 (Week 4): Code Quality
- [ ] Fix #10: Add @Transactional
- [ ] Fix #11: Soft delete consistency
- [ ] Fix #12: API rate limiting
- [ ] Fix #13: Configurable timeouts

### Sprint 4 (Week 5): Polish
- [ ] Fix #14: Structured logging
- [ ] Fix #15: i18n messages
- [ ] Fix #16-25: Minor improvements

---

## 🎯 Kế hoạch Branch & PR

### Create fix branches

```bash
# From develop
git checkout develop
git pull origin develop

# Critical fixes
git checkout -b fix/payment-signature-verification
git checkout -b fix/payment-gateway-integration
git checkout -b fix/payment-webhook
git checkout -b fix/payment-idempotency
git checkout -b fix/token-blacklisting

# Major fixes
git checkout -b fix/booking-pagination
git checkout -b fix/booking-endpoints
git checkout -b fix/redis-connection-pool
git checkout -b fix/email-templates

# Code quality
git checkout -b fix/add-transactional
git checkout -b fix/soft-delete-consistency
git checkout -b fix/api-rate-limiting
```

### Commit convention
```bash
git commit -m "fix(payment): implement signature verification for payment callback

- Add HMAC-SHA512 signature verification
- Prevent fake payment success attacks
- Add security tests

Fixes: #2
BREAKING CHANGE: Payment callback now requires valid signature"
```

---

## 🧪 Testing Checklist

Trước khi merge mỗi fix:

### Unit Tests
- [ ] Service logic tests
- [ ] Repository tests
- [ ] Mapper tests

### Integration Tests
- [ ] API endpoint tests
- [ ] Database integration tests
- [ ] Redis integration tests

### Security Tests
- [ ] Signature verification tests
- [ ] Authentication/Authorization tests
- [ ] Rate limiting tests

### Load Tests (Critical fixes only)
- [ ] Concurrent booking tests
- [ ] Payment callback stress tests
- [ ] Redis connection pool tests

---

## 📝 Conclusion

**Summary:**
- **25 issues** identified
- **5 CRITICAL** (security & payment)
- **8 MAJOR** (features & performance)
- **12 MINOR** (code quality & UX)

**Estimated Total Effort:** 4-6 weeks (1 developer)

**Recommended Approach:**
1. ✅ Fix CRITICAL issues trước (Week 1-2)
2. ✅ Deploy lên staging để test
3. ✅ Fix MAJOR issues (Week 3-4)
4. ✅ Polish với MINOR issues (Week 5+)

**⚠️ Lưu ý:**
- **Không deploy production** trước khi fix xong 5 CRITICAL issues
- Cần có comprehensive tests cho mỗi fix
- Review code kỹ trước khi merge vào develop
- Sau khi fix xong tất cả, merge develop → master

**Next Steps:**
1. Review danh sách issues với team
2. Prioritize theo business requirements
3. Create GitHub issues cho từng item
4. Assign người làm
5. Start Sprint 1!

---

Chúc may mắn với việc fix bugs! 🚀
