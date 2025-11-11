# 🔄 i18n Migration Examples

## Example 1: BookingService

### ❌ Before (Hardcoded)

```java
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    
    private final BookingRepository bookingRepository;
    
    @Override
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đặt vé với ID: " + id));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Đặt vé đã bị hủy");
        }
        
        return BookingMapper.toResponse(booking);
    }
    
    @Override
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đặt vé"));
            
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException("Không thể hủy đặt vé đã xác nhận");
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}
```

### ✅ After (i18n Support)

```java
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    
    private final BookingRepository bookingRepository;
    private final MessageUtils messageUtils; // ← Add this
    
    @Override
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                messageUtils.getMessage("error.booking.not.found", id) // ← Use i18n
            ));
        
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException(
                messageUtils.getMessage("error.booking.already.cancelled") // ← Use i18n
            );
        }
        
        return BookingMapper.toResponse(booking);
    }
    
    @Override
    public void cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                messageUtils.getMessage("error.booking.not.found", id) // ← Use i18n
            ));
            
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BadRequestException(
                messageUtils.getMessage("error.booking.cannot.cancel") // ← Use i18n
            );
        }
        
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }
}
```

---

## Example 2: SeatService

### ❌ Before

```java
@Override
public void holdSeats(Long showtimeId, List<Long> seatIds) {
    for (Long seatId : seatIds) {
        Seat seat = seatRepository.findById(seatId)
            .orElseThrow(() -> new NotFoundException("Seat not found: " + seatId));
            
        if (isSeatBooked(showtimeId, seatId)) {
            throw new BadRequestException("Ghế " + seat.getSeatNumber() + " đã được đặt");
        }
        
        boolean held = redisTemplate.opsForValue().setIfAbsent(
            "seat:hold:" + showtimeId + ":" + seatId,
            "HELD",
            Duration.ofMinutes(5)
        );
        
        if (!held) {
            throw new BadRequestException("Không thể giữ ghế: " + seat.getSeatNumber());
        }
    }
}
```

### ✅ After

```java
@Override
public void holdSeats(Long showtimeId, List<Long> seatIds) {
    for (Long seatId : seatIds) {
        Seat seat = seatRepository.findById(seatId)
            .orElseThrow(() -> new NotFoundException(
                messageUtils.getMessage("error.seat.not.found", seatId)
            ));
            
        if (isSeatBooked(showtimeId, seatId)) {
            throw new BadRequestException(
                messageUtils.getMessage("error.seat.already.booked", seat.getSeatNumber())
            );
        }
        
        boolean held = redisTemplate.opsForValue().setIfAbsent(
            "seat:hold:" + showtimeId + ":" + seatId,
            "HELD",
            Duration.ofMinutes(5)
        );
        
        if (!held) {
            throw new BadRequestException(
                messageUtils.getMessage("error.seat.hold.failed", seat.getSeatNumber())
            );
        }
    }
}
```

---

## Example 3: ShowtimeService

### ❌ Before

```java
@Override
public void validateShowtime(Long showtimeId) {
    Showtime showtime = showtimeRepository.findById(showtimeId)
        .orElseThrow(() -> new NotFoundException("Showtime not found"));
        
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime showtimeStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
    
    if (showtimeStart.isBefore(now)) {
        throw new BadRequestException("Cannot book for past showtime");
    }
    
    if (showtimeStart.isBefore(now.plusMinutes(15))) {
        throw new BadRequestException("Showtime must be at least 15 minutes in future");
    }
}
```

### ✅ After

```java
@Override
public void validateShowtime(Long showtimeId) {
    Showtime showtime = showtimeRepository.findById(showtimeId)
        .orElseThrow(() -> new NotFoundException(
            messageUtils.getMessage("error.showtime.not.found", showtimeId)
        ));
        
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime showtimeStart = LocalDateTime.of(showtime.getShowDate(), showtime.getStartTime());
    
    if (showtimeStart.isBefore(now)) {
        throw new BadRequestException(
            messageUtils.getMessage("error.showtime.past")
        );
    }
    
    if (showtimeStart.isBefore(now.plusMinutes(15))) {
        throw new BadRequestException(
            messageUtils.getMessage("error.showtime.invalid.time")
        );
    }
}
```

---

## Example 4: PaymentService

### ❌ Before

```java
@Override
public PaymentResponse processPayment(PaymentRequest request) {
    Booking booking = bookingRepository.findById(request.getBookingId())
        .orElseThrow(() -> new NotFoundException("Booking not found"));
        
    if (booking.getTotalPrice().compareTo(request.getAmount()) != 0) {
        throw new BadRequestException("Invalid payment amount: " + request.getAmount());
    }
    
    if (booking.getStatus() == BookingStatus.CONFIRMED) {
        throw new BadRequestException("Payment already processed");
    }
    
    // Process payment...
    
    return response;
}
```

### ✅ After

```java
@Override
public PaymentResponse processPayment(PaymentRequest request) {
    Booking booking = bookingRepository.findById(request.getBookingId())
        .orElseThrow(() -> new NotFoundException(
            messageUtils.getMessage("error.booking.not.found", request.getBookingId())
        ));
        
    if (booking.getTotalPrice().compareTo(request.getAmount()) != 0) {
        throw new BadRequestException(
            messageUtils.getMessage("error.payment.invalid.amount", request.getAmount())
        );
    }
    
    if (booking.getStatus() == BookingStatus.CONFIRMED) {
        throw new BadRequestException(
            messageUtils.getMessage("error.payment.already.processed")
        );
    }
    
    // Process payment...
    
    return response;
}
```

---

## Example 5: UserService

### ❌ Before

```java
@Override
public UserResponse createUser(UserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new ConflictException("Email already exists: " + request.getEmail());
    }
    
    if (userRepository.existsByPhone(request.getPhone())) {
        throw new ConflictException("Phone already exists: " + request.getPhone());
    }
    
    User user = UserMapper.toEntity(request);
    userRepository.save(user);
    
    return UserMapper.toResponse(user);
}
```

### ✅ After

```java
@Override
public UserResponse createUser(UserRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
        throw new ConflictException(
            messageUtils.getMessage("error.user.email.exists", request.getEmail())
        );
    }
    
    if (userRepository.existsByPhone(request.getPhone())) {
        throw new ConflictException(
            messageUtils.getMessage("error.user.phone.exists", request.getPhone())
        );
    }
    
    User user = UserMapper.toEntity(request);
    userRepository.save(user);
    
    return UserMapper.toResponse(user);
}
```

---

## 🎯 Migration Checklist

For each Service class:

- [ ] Add `private final MessageUtils messageUtils;` to constructor
- [ ] Find all `throw new NotFoundException("...")` → Replace with message key
- [ ] Find all `throw new BadRequestException("...")` → Replace with message key
- [ ] Find all `throw new ConflictException("...")` → Replace with message key
- [ ] Test with `Accept-Language: vi`
- [ ] Test with `Accept-Language: en`
- [ ] Verify error messages display correctly in both languages

---

## 📋 Quick Reference

### Common Patterns

```java
// Not Found
.orElseThrow(() -> new NotFoundException(
    messageUtils.getMessage("error.{entity}.not.found", id)
))

// Already Exists
if (exists) {
    throw new ConflictException(
        messageUtils.getMessage("error.{entity}.{field}.exists", value)
    );
}

// Already Done
if (alreadyDone) {
    throw new BadRequestException(
        messageUtils.getMessage("error.{entity}.already.{action}")
    );
}

// Cannot Do
if (cannotDo) {
    throw new BadRequestException(
        messageUtils.getMessage("error.{entity}.cannot.{action}")
    );
}

// Invalid
if (invalid) {
    throw new BadRequestException(
        messageUtils.getMessage("error.{entity}.invalid.{field}", value)
    );
}
```

---

## 🧪 Testing Example

```java
@Test
void testBookingNotFound_Vietnamese() {
    // Setup
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Test with Vietnamese locale
    LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
    
    // Execute & Verify
    NotFoundException ex = assertThrows(NotFoundException.class, () -> {
        bookingService.getBookingById(999L);
    });
    
    assertEquals("Không tìm thấy đặt vé với ID: 999", ex.getMessage());
}

@Test
void testBookingNotFound_English() {
    // Setup
    when(bookingRepository.findById(999L)).thenReturn(Optional.empty());
    
    // Test with English locale
    LocaleContextHolder.setLocale(Locale.forLanguageTag("en"));
    
    // Execute & Verify
    NotFoundException ex = assertThrows(NotFoundException.class, () -> {
        bookingService.getBookingById(999L);
    });
    
    assertEquals("Booking not found with ID: 999", ex.getMessage());
}
```

---

**Status:** ✅ Ready for migration  
**Priority:** 🟢 MINOR (Can be done incrementally)  
**Estimated Time:** 2-3 hours for full codebase migration
