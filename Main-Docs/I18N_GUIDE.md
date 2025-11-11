# 🌍 Internationalization (i18n) Guide

## Overview
The system supports **Vietnamese** (default) and **English** languages for all error messages, validation messages, and user-facing text.

---

## 📁 File Structure

```
src/main/resources/
├── messages.properties          # English messages (default)
├── messages_vi.properties       # Vietnamese messages
```

```java
src/main/java/com/trainning/movie_booking_system/
├── config/
│   └── LocaleConfig.java       # i18n configuration
└── utils/
    └── MessageUtils.java       # Message helper utility
```

---

## ⚙️ Configuration

### 1. Locale Configuration (`LocaleConfig.java`)

```java
@Bean
public LocaleResolver localeResolver() {
    AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
    localeResolver.setDefaultLocale(Locale.forLanguageTag("vi")); // Default: Vietnamese
    localeResolver.setSupportedLocales(List.of(
        Locale.forLanguageTag("vi"),
        Locale.forLanguageTag("en")
    ));
    return localeResolver;
}
```

**Supported Locales:**
- `vi` - Vietnamese (default)
- `en` - English

---

## 📝 Message Files

### English (`messages.properties`)
```properties
error.booking.not.found=Booking not found with ID: {0}
error.seat.already.booked=Seat {0} is already booked
success.booking.created=Booking created successfully
```

### Vietnamese (`messages_vi.properties`)
```properties
error.booking.not.found=Không tìm thấy đặt vé với ID: {0}
error.seat.already.booked=Ghế {0} đã được đặt
success.booking.created=Đặt vé thành công
```

---

## 🚀 Usage

### Method 1: Using `MessageUtils` (Recommended)

```java
@Service
public class BookingServiceImpl {
    
    private final MessageUtils messageUtils;
    
    public void createBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException(
                messageUtils.getMessage("error.booking.not.found", bookingId)
            ));
    }
    
    public void checkSeat(String seatNumber) {
        if (isSeatBooked(seatNumber)) {
            throw new BadRequestException(
                messageUtils.getMessage("error.seat.already.booked", seatNumber)
            );
        }
    }
}
```

### Method 2: Using `MessageSource` Directly

```java
@Service
public class BookingServiceImpl {
    
    private final MessageSource messageSource;
    
    public void createBooking(Long bookingId) {
        String message = messageSource.getMessage(
            "error.booking.not.found",
            new Object[]{bookingId},
            LocaleContextHolder.getLocale()
        );
        throw new NotFoundException(message);
    }
}
```

---

## 🌐 Client Usage

### Option 1: Accept-Language Header (Recommended)

```bash
# Vietnamese (default)
curl -H "Accept-Language: vi" http://localhost:8080/api/bookings/1

# English
curl -H "Accept-Language: en" http://localhost:8080/api/bookings/1
```

**Response (Vietnamese):**
```json
{
  "status": 404,
  "message": "Không tìm thấy đặt vé với ID: 1",
  "timestamp": "2025-11-11T10:30:00"
}
```

**Response (English):**
```json
{
  "status": 404,
  "message": "Booking not found with ID: 1",
  "timestamp": "2025-11-11T10:30:00"
}
```

### Option 2: Query Parameter

```bash
# Vietnamese
GET http://localhost:8080/api/bookings/1?lang=vi

# English
GET http://localhost:8080/api/bookings/1?lang=en
```

---

## 📋 Message Categories

### 1. Authentication & Authorization
```properties
error.auth.invalid.credentials
error.auth.email.exists
error.auth.account.locked
error.auth.unauthorized
error.auth.forbidden
error.auth.token.invalid
error.auth.token.expired
```

### 2. Booking
```properties
error.booking.not.found
error.booking.already.confirmed
error.booking.already.cancelled
error.booking.expired
error.booking.cannot.cancel
error.booking.invalid.status
error.booking.payment.pending
```

### 3. Seat
```properties
error.seat.not.found
error.seat.already.booked
error.seat.not.available
error.seat.hold.expired
error.seat.hold.failed
error.seat.invalid.type
```

### 4. Showtime
```properties
error.showtime.not.found
error.showtime.past
error.showtime.full
error.showtime.not.started
error.showtime.ended
error.showtime.invalid.time
```

### 5. Payment
```properties
error.payment.not.found
error.payment.failed
error.payment.invalid.amount
error.payment.already.processed
error.payment.callback.invalid
error.payment.timeout
```

### 6. Voucher
```properties
error.voucher.not.found
error.voucher.expired
error.voucher.not.started
error.voucher.used
error.voucher.limit.reached
error.voucher.min.amount
```

### 7. Validation
```properties
error.validation.required
error.validation.invalid
error.validation.min.length
error.validation.max.length
error.validation.email
error.validation.phone
error.validation.date.past
error.validation.date.format
```

### 8. Success Messages
```properties
success.booking.created
success.booking.confirmed
success.booking.cancelled
success.payment.success
success.seat.hold
success.seat.release
```

---

## 🔧 Adding New Messages

### Step 1: Add to `messages.properties` (English)
```properties
error.movie.not.available=Movie {0} is not available for booking
```

### Step 2: Add to `messages_vi.properties` (Vietnamese)
```properties
error.movie.not.available=Phim {0} không khả dụng để đặt vé
```

### Step 3: Use in Code
```java
throw new BadRequestException(
    messageUtils.getMessage("error.movie.not.available", movieName)
);
```

---

## 🧪 Testing

### Test with Postman

**Vietnamese:**
```
GET /api/bookings/999
Headers:
  Accept-Language: vi
```

**English:**
```
GET /api/bookings/999
Headers:
  Accept-Language: en
```

### Test with cURL

```bash
# Vietnamese
curl -H "Accept-Language: vi" http://localhost:8080/api/bookings/999

# English
curl -H "Accept-Language: en" http://localhost:8080/api/bookings/999
```

---

## 📊 Example Conversions

### Before (Hardcoded)
```java
// ❌ Bad - Hardcoded Vietnamese
throw new NotFoundException("Không tìm thấy đặt vé với ID: " + id);

// ❌ Bad - Hardcoded English
throw new BadRequestException("Seat already booked");
```

### After (i18n)
```java
// ✅ Good - Supports both languages
throw new NotFoundException(
    messageUtils.getMessage("error.booking.not.found", id)
);

throw new BadRequestException(
    messageUtils.getMessage("error.seat.already.booked", seatNumber)
);
```

---

## 🎯 Best Practices

1. **Always use message keys**, never hardcode messages
2. **Use MessageUtils** for cleaner code
3. **Keep message keys descriptive** (`error.booking.not.found` not `err1`)
4. **Use placeholders** for dynamic values (`{0}`, `{1}`)
5. **Maintain both language files** in sync
6. **Test both languages** before committing

---

## 🔍 Common Patterns

### Pattern 1: Not Found
```java
Entity entity = repository.findById(id)
    .orElseThrow(() -> new NotFoundException(
        messageUtils.getMessage("error.{entity}.not.found", id)
    ));
```

### Pattern 2: Already Exists
```java
if (repository.existsByEmail(email)) {
    throw new ConflictException(
        messageUtils.getMessage("error.user.email.exists", email)
    );
}
```

### Pattern 3: Invalid State
```java
if (booking.getStatus() == BookingStatus.CONFIRMED) {
    throw new BadRequestException(
        messageUtils.getMessage("error.booking.already.confirmed")
    );
}
```

---

## 🌟 Benefits

✅ **Multilingual Support** - Easy to add more languages  
✅ **Maintainability** - Centralized message management  
✅ **Consistency** - Same message across the app  
✅ **User Experience** - Users see messages in their language  
✅ **Testing** - Easy to test different languages  

---

## 📚 References

- [Spring Boot i18n Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-factory-message-source)
- [MessageSource API](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/MessageSource.html)
- [LocaleContextHolder](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/i18n/LocaleContextHolder.html)

---

## 🚨 Migration Checklist

- [ ] Update all `throw new NotFoundException("...")` → use messageUtils
- [ ] Update all `throw new BadRequestException("...")` → use messageUtils
- [ ] Update all `throw new ConflictException("...")` → use messageUtils
- [ ] Test with `Accept-Language: vi` header
- [ ] Test with `Accept-Language: en` header
- [ ] Verify Postman collection works with both languages
- [ ] Update API documentation with i18n examples

---

**Created:** 2025-11-11  
**Last Updated:** 2025-11-11  
**Status:** ✅ IMPLEMENTED
