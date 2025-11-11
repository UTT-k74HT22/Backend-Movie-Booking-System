# 🔍 NEW ISSUES FOUND - Code Review Report

> **Branch:** `refactor/cleanup-docs-and-logs`  
> **Date:** 2025-11-11  
> **Reviewer:** GitHub Copilot  

---

## 📋 SUMMARY

Đã review toàn bộ codebase và phát hiện **13 vấn đề mới** ngoài 12 bugs đã biết:

| Category | Count | Priority |
|----------|-------|----------|
| 🔴 **Security Issues** | 3 | CRITICAL |
| 🟡 **Code Quality** | 5 | MAJOR |
| 🟢 **Documentation** | 3 | MINOR |
| 🔵 **Cleanup Tasks** | 2 | LOW |
| **TOTAL** | **13** | - |

---

## 🔴 SECURITY ISSUES (3)

### SECURITY #1: Payment Signature Not Verified ⚠️ CRITICAL
**File:** `PaymentServiceImpl.java:179`  
**Severity:** 🔴 CRITICAL - SECURITY VULNERABILITY  
**Impact:** Attacker có thể giả mạo payment success

**Current Code:**
```java
@Override
@Transactional
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // TODO: CRITICAL - VERIFY SIGNATURE
    // ==================================
    // if (!verifyPaymentSignature(request)) {
    //     log.error("[PAYMENT] ⚠️ SECURITY: Invalid payment signature for booking {}", request.getBookingId());
    //     throw new SecurityException("Invalid payment signature");
    // }
    
    // DANGEROUS: Processing payment without verification!
    if ("SUCCESS".equals(request.getStatus())) {
        booking.setStatus(BookingStatus.CONFIRMED);
        // ...
    }
}
```

**Risk:**
- Attacker gửi fake request: `POST /api/payments/callback?bookingId=123&status=SUCCESS`
- Booking được confirm mà không payment thực sự
- Mất tiền, fraud, security breach

**Solution:**
```java
// 1. Add signature parameter to PaymentRequest
@Data
public class PaymentRequest {
    private Long bookingId;
    private String status;
    private String transactionId;
    private String signature; // Required!
}

// 2. Implement verification
private boolean verifyPaymentSignature(PaymentRequest request) {
    String data = request.getBookingId() + "|" 
                + request.getStatus() + "|" 
                + request.getTransactionId();
    
    String calculatedHash = HmacSHA512(hashSecret, data);
    return calculatedHash.equals(request.getSignature());
}

// 3. Verify before processing
if (!verifyPaymentSignature(request)) {
    log.error("[SECURITY] Invalid payment signature for booking {}", request.getBookingId());
    throw new SecurityException("Invalid payment signature");
}
```

**Estimated Fix:** 2-3 hours  
**Related Bug:** BUG #5 - Payment IPN/Webhook

---

### SECURITY #2: No Idempotency Check ⚠️ CRITICAL
**File:** `PaymentServiceImpl.java:189`  
**Severity:** 🔴 CRITICAL  
**Impact:** Double payment processing, duplicate bookings

**Current Code:**
```java
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // TODO: Check idempotency - prevent double processing
    // ====================================================
    // if (paymentTransactionRepository.existsByTransactionId(request.getTransactionId())) {
    //     log.warn("[PAYMENT] Duplicate transaction ID: {}", request.getTransactionId());
    //     return buildResponse(booking, "DUPLICATE");
    // }
    
    // DANGEROUS: No duplicate check!
    booking.setStatus(BookingStatus.CONFIRMED);
}
```

**Risk:**
- Gateway sends webhook twice (network retry)
- User refreshes payment page
- Same booking confirmed multiple times

**Estimated Fix:** Covered in BUG #4  
**Related Bug:** BUG #4 - Payment Idempotency

---

### SECURITY #3: No Amount Validation ⚠️ CRITICAL
**File:** `PaymentServiceImpl.java:196`  
**Severity:** 🔴 CRITICAL  
**Impact:** User pays $1 but gets $100 booking

**Current Code:**
```java
public PaymentResponse handlePaymentCallback(PaymentRequest request) {
    // TODO: Validate amount matches booking.totalPrice
    // =================================================
    // BigDecimal receivedAmount = new BigDecimal(request.getAmount());
    // if (receivedAmount.compareTo(booking.getTotalPrice()) != 0) {
    //     log.error("[PAYMENT] Amount mismatch! Expected: {}, Received: {}", 
    //         booking.getTotalPrice(), receivedAmount);
    //     throw new BadRequestException("Payment amount mismatch");
    // }
    
    // DANGEROUS: No amount verification!
    booking.setStatus(BookingStatus.CONFIRMED);
}
```

**Solution:**
```java
// Add amount to PaymentRequest
@Data
public class PaymentRequest {
    private Long bookingId;
    private String status;
    private BigDecimal amount; // Required!
    private String signature;
}

// Verify amount
BigDecimal expectedAmount = booking.getTotalPrice();
if (request.getAmount().compareTo(expectedAmount) != 0) {
    log.error("[PAYMENT] Amount mismatch! Booking {}: Expected {}, Got {}", 
        booking.getId(), expectedAmount, request.getAmount());
    throw new BadRequestException("Payment amount mismatch");
}
```

**Estimated Fix:** 1 hour  
**Priority:** 🔴 FIX IMMEDIATELY

---

## 🟡 CODE QUALITY ISSUES (5)

### QUALITY #1: Emoji Icons in Logs 🎬⚠️🔥
**Files:** Multiple files (12 matches found)  
**Severity:** 🟡 MAJOR  
**Impact:** Log parsing issues, unprofessional

**Found in:**
- `PaymentServiceImpl.java` - 6 matches
- `PaymentController.java` - 4 matches  
- `BookingServiceImpl.java` (potential)

**Examples:**
```java
log.warn("[PAYMENT] ⚠️ TODO: Using MOCK payment URL. Implement real gateway!");
log.info("[PAYMENT] ✅ Payment SUCCESS for booking {}", booking.getId());
log.warn("[PAYMENT] ❌ Payment FAILED for booking {}", booking.getId());
log.error("[PAYMENT] ⚠️ SECURITY: Invalid payment signature");
```

**Problems:**
- Emoji không hiển thị đúng trong log aggregation tools (ELK, Splunk)
- Không parse được bởi log parsers
- Không professional cho production logs
- Khó grep/search trong terminal

**Solution:**
```java
// Replace with standard log levels and markers
log.warn("[PAYMENT] [TODO] Using MOCK payment URL. Implement real gateway!");
log.info("[PAYMENT] [SUCCESS] Payment successful for booking {}", booking.getId());
log.warn("[PAYMENT] [FAILED] Payment failed for booking {}", booking.getId());
log.error("[PAYMENT] [SECURITY] Invalid payment signature for booking {}", request.getBookingId());
```

**Estimated Fix:** 30 minutes  
**Action:** Remove ALL emojis from log statements

---

### QUALITY #2: Using `e.getMessage()` Without Stack Trace
**Files:** 9 matches across multiple files  
**Severity:** 🟡 MAJOR  
**Impact:** Lost debugging information

**Found in:**
- `MailServiceImpl.java:59`
- `AuthServiceImpl.java:132, 143, 152, 155, 158`
- `JwtFilter.java:49, 85`
- `BookingExpireService.java:68`

**Bad Pattern:**
```java
catch (Exception e) {
    log.error("SendMail failed: {}", e.getMessage(), e);  // ✅ Good - has stack trace
    log.error("Error: {}", e.getMessage());               // ❌ Bad - no stack trace
}
```

**Examples:**
```java
// MailServiceImpl.java:59
log.error("SendMail failed: {}", e.getMessage(), e); // ✅ GOOD

// JwtFilter.java:49
log.warn("Failed to extract username from token: {}", e.getMessage()); // ❌ BAD

// JwtFilter.java:85  
log.error("Cannot set user authentication: {}", e.getMessage()); // ❌ BAD
```

**Solution:**
```java
// Always include exception object for stack trace
log.error("Error occurred: {}", e.getMessage(), e);

// Or use SLF4J's exception parameter
log.error("Error occurred", e);
```

**Estimated Fix:** 15 minutes  
**Action:** Add exception parameter to all log.error() calls

---

### QUALITY #3: Inconsistent Log Prefixes
**Severity:** 🟡 MAJOR  
**Impact:** Hard to filter logs by component

**Current State:**
```java
// Different prefixes across files
log.info("[PAYMENT] ...");
log.info("[BOOKING] ...");
log.info("Activating account for email: {}"); // No prefix
log.info("Creating roles if not exists..."); // No prefix
```

**Solution:** Standardize all log messages with component prefix
```java
// Use consistent pattern
log.info("[AUTH] Activating account for email: {}", email);
log.info("[INIT] Creating roles if not exists");
log.info("[PAYMENT] Creating payment URL for booking {}", bookingId);
```

**Estimated Fix:** 1 hour  
**Action:** Add component prefixes to all log statements

---

### QUALITY #4: Vietnamese Comments in Critical Code
**Files:** Multiple  
**Severity:** 🟢 MINOR  
**Impact:** International team collaboration issues

**Examples:**
```java
// RoleType.java
/*
* Giải thích : code : mã định danh role
*            description : mô tả role
*           level : cấp độ quyền hạn (số càng cao quyền càng lớn)
* */

// ApplicationInitConfig.java
.description("Người dùng thông thường")
.description("Nhân viên")
.description("Quản lý rạp chiếu phim")

// BookingServiceImpl.java
throw new ConflictException("Không thể lock ghế %d. Vui lòng thử lại.".formatted(seatId));
```

**Recommendation:**
- Keep code/logs in English for international standards
- Use i18n for user-facing messages
- Technical comments should be English

**Estimated Fix:** 2 hours  
**Priority:** 🟢 OPTIONAL (can do later)

---

### QUALITY #5: Hardcoded CORS Origins
**File:** `SecurityConfig.java:88`  
**Severity:** 🟡 MAJOR  
**Impact:** Cannot deploy to production without code change

**Current Code:**
```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000")); // HARDCODED!
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

**Solution:**
```yaml
# application.yml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

```java
@Value("${cors.allowed-origins}")
private String[] allowedOrigins;

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigins));
    // ... rest
}
```

**Estimated Fix:** 15 minutes  
**Priority:** 🟡 MAJOR - Should fix before production

---

## 🟢 DOCUMENTATION ISSUES (3)

### DOC #1: Duplicate/Old Documentation Files
**Severity:** 🟢 MINOR  
**Impact:** Confusion, maintenance overhead

**Found:**
- `API_USAGE_EXAMPLE.md` (root) vs `Main-Docs/API_USAGE_EXAMPLE.md`
- `BOOKING_SEQUENCE_DIAGRAM.md` (root) vs `Main-Docs/BOOKING_SEQUENCE_DIAGRAM.md`
- `JWT_AUTHENTICATION_GUIDE.md` (root) vs `Main-Docs/JWT_AUTHENTICATION_GUIDE.md`
- `API_TESTING_GUIDE.md` (root) vs `Main-Docs/API_TESTING_GUIDE.md`
- `TESTING_GUIDE.md` (root) vs `Main-Docs/TESTING_GUIDE.md`
- `EXCEPTION.md` (root) vs `Main-Docs/EXCEPTION.md`

**Plus outdated docs:**
- `CHECKLIST_BUG_1.md` - Completed
- `CHECKLIST_BUG_2.md` - Completed
- `CHECKLIST_BUG_3.md` - Completed
- `TASK_BUG_1_SHOWTIME_VALIDATION.md` - Completed
- `TASK_BUG_2_BOOKING_SEAT_FIELDS.md` - Completed
- `TASK_BUG_3_BOOKING_EXPIRATION.md` - Completed
- `BUG_3_MIGRATION_GUIDE.md` - Completed
- `IMPLEMENTATION_FIXED.md` - Old

**Action:** DELETE or MOVE to archive

---

### DOC #2: Payment Documentation Missing
**Severity:** 🟢 MINOR  
**Impact:** No deployment guide for payment integration

**Missing:**
- VNPay integration guide
- Payment gateway credentials setup
- Webhook configuration guide
- Payment testing guide

**Action:** Will create after implementing real payment gateway (BUG #6)

---

### DOC #3: Payment-Voucher Docs in Root
**Location:** `Payment-Voucher/` folder  
**Severity:** 🟢 MINOR  
**Impact:** Not yet implemented, keep for future

**Files:**
- 10 markdown files about payment voucher feature
- Not implemented yet
- Keep for future reference

**Action:** KEEP (as mentioned by user)

---

## 🔵 CLEANUP TASKS (2)

### CLEANUP #1: Remove TODO Comments (Non-Payment/Voucher)
**Found:** 21 TODO comments  
**Action:**
- Keep payment/voucher TODOs (work in progress)
- Remove completed TODOs
- Convert important TODOs to GitHub Issues

**Files with TODOs:**
- `PaymentService.java` - 5 TODOs (KEEP - payment implementation)
- `PaymentServiceImpl.java` - 16 TODOs (KEEP - payment implementation)

**No action needed** - All TODOs are for payment feature (work in progress)

---

### CLEANUP #2: Remove Emoji from All Logs
**Action:** Already covered in QUALITY #1

---

## 📝 CLEANUP PLAN

### Phase 1: Critical Fixes (Do NOT do in this branch)
These are actual bugs that need separate branches:
- ❌ SECURITY #1: Payment Signature (needs BUG #5)
- ❌ SECURITY #2: Idempotency (needs BUG #4)  
- ❌ SECURITY #3: Amount Validation (can add to BUG #5)

### Phase 2: This Branch - Cleanup & Quality
**Tasks for `refactor/cleanup-docs-and-logs`:**

✅ **DO:**
1. Remove emoji icons from ALL log statements
2. Fix log statements without stack traces
3. Standardize log prefixes
4. Move CORS config to application.yml
5. Delete duplicate/old documentation:
   - `CHECKLIST_BUG_*.md` (3 files)
   - `TASK_BUG_*.md` (3 files)
   - `BUG_3_MIGRATION_GUIDE.md`
   - Duplicate docs already in Main-Docs/
6. Keep comment in English (optional - can skip)

❌ **DO NOT:**
- Touch payment-related TODOs
- Touch voucher-related files
- Implement security fixes (needs separate branches)

---

## 🎯 EXECUTION ORDER

**This Branch (`refactor/cleanup-docs-and-logs`):**
1. ✅ Create this report
2. ⏳ Remove emojis from logs
3. ⏳ Fix log.error() without stack traces
4. ⏳ Standardize log prefixes
5. ⏳ Externalize CORS config
6. ⏳ Delete old/duplicate docs
7. ⏳ Commit & PR

**Future Branches (Security Fixes):**
- `bugfix/payment-signature-validation` - SECURITY #1 & #3
- `bugfix/payment-idempotency` - SECURITY #2 (BUG #4)

**Later (Optional):**
- `refactor/english-comments` - QUALITY #4
- `docs/payment-integration-guide` - DOC #2

---

## ⏱️ TIME ESTIMATE

**This Branch:** 2-3 hours  
**Security Fixes:** 5-8 hours (separate branches)  
**Total:** 7-11 hours

---

**Ready to start cleanup?** 🚀
