# 🎯 TASK: Fix BUG #2 - BookingSeat Denormalized Fields

> **Branch:** `bugfix/booking-seat-denormalized-fields` (✅ Đã tạo từ `develop`)  
> **Assignee:** Bạn  
> **Estimate:** 1-2 giờ  
> **Priority:** 🔴 CRITICAL  
> **Status:** 🟡 IN PROGRESS

---

## 📝 PROBLEM DESCRIPTION

**Hiện tại:**
- `BookingSeat` entity chỉ có `seat_id` (FK)
- Không có thông tin display: `seatNumber`, `rowLabel`, `seatType`
- Frontend phải join với `Seat` table → **N+1 query problem**
- API response thiếu thông tin ghế → UX kém

**Current Schema:**
```sql
booking_seats:
- id
- booking_id (FK)
- seat_id (FK)
- price

❌ MISSING: seat_number, row_label, seat_type
```

**Ví dụ vấn đề:**
```json
// ❌ Response hiện tại
{
  "id": 1,
  "bookingId": 100,
  "seatId": 5,
  "price": 150000
  // ❌ Thiếu: "A1", "VIP", etc.
}

// Frontend phải:
GET /api/seats/5  // Extra query!
```

---

## 🎯 ACCEPTANCE CRITERIA

### ✅ Scenario 1: Database schema có denormalized fields
```sql
DESC booking_seats;

Expected columns:
- seat_number INT
- row_label VARCHAR(10)
- seat_type VARCHAR(20)
```

### ✅ Scenario 2: Entity có fields mới
```java
BookingSeat entity:
- int seatNumber
- String rowLabel
- SeatType seatType
```

### ✅ Scenario 3: API response có seat info
```json
GET /api/bookings/1

{
  "seats": [
    {
      "id": 1,
      "seatNumber": 1,
      "rowLabel": "A",
      "seatType": "STANDARD",
      "seatLabel": "A1"  // ✅ Computed field
    }
  ]
}
```

### ✅ Scenario 4: Existing data được migrate
```sql
-- Data cũ vẫn có đầy đủ thông tin
SELECT seat_number, row_label FROM booking_seats WHERE id < 100;
-- Returns: A1, B2, C3, etc.
```

---

## 📂 FILES TO MODIFY

### 1. Database Migration (BẠN LÀM TRƯỚC)
**File:** Tạo mới `src/main/resources/db/migration/V2__add_booking_seat_denormalized_fields.sql`

### 2. Entity Layer
**File:** `src/main/java/.../entity/BookingSeat.java`

### 3. DTO Layer
**File:** `src/main/java/.../dto/response/Booking/BookingSeatDTO.java`

### 4. Service Layer
**File:** `src/main/java/.../service/impl/BookingServiceImpl.java`

---

## 💡 IMPLEMENTATION GUIDE

### Step 1: Create Database Migration

**File:** `src/main/resources/db/migration/V2__add_booking_seat_denormalized_fields.sql`

```sql
-- Add denormalized seat info to booking_seats table
ALTER TABLE booking_seats 
ADD COLUMN seat_number INT NOT NULL DEFAULT 0 AFTER seat_id,
ADD COLUMN row_label VARCHAR(10) NOT NULL DEFAULT '' AFTER seat_number,
ADD COLUMN seat_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD' AFTER row_label;

-- Migrate existing data from seats table
UPDATE booking_seats bs
INNER JOIN seats s ON bs.seat_id = s.id
SET 
    bs.seat_number = s.seat_number,
    bs.row_label = s.row_label,
    bs.seat_type = s.seat_type;

-- Remove default values (they were only for migration)
ALTER TABLE booking_seats 
ALTER COLUMN seat_number DROP DEFAULT,
ALTER COLUMN row_label DROP DEFAULT,
ALTER COLUMN seat_type DROP DEFAULT;

-- Add index for better query performance
CREATE INDEX idx_booking_seat_info ON booking_seats(row_label, seat_number);
```

### Step 2: Update BookingSeat Entity

**File:** `src/main/java/com/trainning/movie_booking_system/entity/BookingSeat.java`

**Current code:**
```java
@Entity
@Table(name = "booking_seats")
public class BookingSeat extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    
    // ❌ MISSING: seatNumber, rowLabel, seatType
}
```

**TODO: Thêm fields:**
```java
// TODO: Add after price field

@Column(name = "seat_number", nullable = false)
private int seatNumber;

@Column(name = "row_label", nullable = false, length = 10)
private String rowLabel;

@Enumerated(EnumType.STRING)
@Column(name = "seat_type", nullable = false, length = 20)
private SeatType seatType;
```

**TODO: Thêm index annotation:**
```java
@Table(
    name = "booking_seats",
    indexes = {
        @Index(name = "idx_booking", columnList = "booking_id"),
        @Index(name = "idx_seat", columnList = "seat_id"),
        @Index(name = "idx_seat_info", columnList = "row_label, seat_number")
    }
)
```

### Step 3: Update BookingSeatDTO

**File:** `src/main/java/com/trainning/movie_booking_system/dto/response/Booking/BookingSeatDTO.java`

**Current:**
```java
@Data
@Builder
public class BookingSeatDTO {
    private Long id;
    private Long seatId;
    private BigDecimal price;
    
    // ❌ MISSING display fields
}
```

**TODO: Thêm fields:**
```java
// TODO: Add after price

private int seatNumber;
private String rowLabel;
private SeatType seatType;

/**
 * Computed field: "A1", "B2", "C3"
 */
public String getSeatLabel() {
    return rowLabel + seatNumber;
}
```

### Step 4: Update Service - Copy Seat Info

**File:** `BookingServiceImpl.java`

**Tìm method `createBookingTransaction()` → vòng lặp tạo BookingSeat:**

**Current:**
```java
for (SeatInfo info : seatInfos) {
    BigDecimal multiplier = (info.getSeatType() == SeatType.VIP) 
        ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
    BigDecimal price = showtime.getPrice()
        .multiply(multiplier)
        .setScale(2, RoundingMode.HALF_UP);

    bookingSeats.add(BookingSeat.builder()
            .booking(booking)
            .seat(seatIdToEntity.get(info.getSeatId()))
            .price(price)
            .build());  // ❌ Missing seat info
    
    total = total.add(price);
}
```

**TODO: Sửa lại:**
```java
for (SeatInfo info : seatInfos) {
    Seat seat = seatIdToEntity.get(info.getSeatId());
    
    BigDecimal multiplier = (info.getSeatType() == SeatType.VIP) 
        ? BigDecimal.valueOf(1.3) : BigDecimal.ONE;
    BigDecimal price = showtime.getPrice()
        .multiply(multiplier)
        .setScale(2, RoundingMode.HALF_UP);

    // TODO: Thêm denormalized fields
    bookingSeats.add(BookingSeat.builder()
            .booking(booking)
            .seat(seat)
            .price(price)
            // ✅ ADD: Copy seat display info
            .seatNumber(seat.getSeatNumber())
            .rowLabel(seat.getRowLabel())
            .seatType(seat.getSeatType())
            .build());
    
    total = total.add(price);
}
```

### Step 5: Update Mapper (If using MapStruct)

**File:** `BookingMapper.java`

```java
@Mapping(target = "seatLabel", 
         expression = "java(bookingSeat.getRowLabel() + bookingSeat.getSeatNumber())")
BookingSeatDTO toBookingSeatDTO(BookingSeat bookingSeat);
```

---

## 🧪 TESTING TASKS

### Task 1: Database Migration Test

```bash
# Run migration
mvn flyway:migrate

# Or start application (if using JPA auto-update)
mvn spring-boot:run

# Connect to MySQL
mysql -u root -p movie_booking

# Verify schema
DESC booking_seats;

Expected output:
+-------------+--------------+------+-----+---------+
| Field       | Type         | Null | Key | Default |
+-------------+--------------+------+-----+---------+
| id          | bigint       | NO   | PRI | NULL    |
| booking_id  | bigint       | NO   | MUL | NULL    |
| seat_id     | bigint       | NO   | MUL | NULL    |
| price       | decimal(12,2)| NO   |     | NULL    |
| seat_number | int          | NO   |     | NULL    | ✅
| row_label   | varchar(10)  | NO   | MUL | NULL    | ✅
| seat_type   | varchar(20)  | NO   |     | NULL    | ✅
+-------------+--------------+------+-----+---------+

# Verify existing data migrated
SELECT id, seat_number, row_label, seat_type FROM booking_seats LIMIT 5;
```

### Task 2: Entity Test

```java
@Test
void testBookingSeatHasDenormalizedFields() {
    Seat seat = Seat.builder()
        .seatNumber(1)
        .rowLabel("A")
        .seatType(SeatType.VIP)
        .build();
    
    BookingSeat bookingSeat = BookingSeat.builder()
        .seat(seat)
        .seatNumber(seat.getSeatNumber())
        .rowLabel(seat.getRowLabel())
        .seatType(seat.getSeatType())
        .price(BigDecimal.valueOf(150000))
        .build();
    
    assertEquals(1, bookingSeat.getSeatNumber());
    assertEquals("A", bookingSeat.getRowLabel());
    assertEquals(SeatType.VIP, bookingSeat.getSeatType());
}
```

### Task 3: Integration Test

```bash
# Start application
mvn spring-boot:run

# Test với Postman
POST http://localhost:8080/api/bookings
{
  "showtimeId": 1,
  "seatIds": [1, 2]
}

Expected Response:
{
  "id": 100,
  "seats": [
    {
      "id": 1,
      "seatId": 1,
      "price": 150000,
      "seatNumber": 1,        ✅
      "rowLabel": "A",        ✅
      "seatType": "STANDARD", ✅
      "seatLabel": "A1"       ✅
    }
  ]
}

# Verify in database
SELECT * FROM booking_seats WHERE booking_id = 100;
```

---

## 🔍 VALIDATION CHECKLIST

- [ ] Migration file created in correct folder
- [ ] Migration script runs successfully
- [ ] Existing data migrated correctly
- [ ] BookingSeat entity has 3 new fields
- [ ] BookingSeatDTO has 3 new fields + computed seatLabel
- [ ] Service copies seat info when creating booking
- [ ] All tests pass
- [ ] API response includes seat display info
- [ ] No N+1 query (verify with SQL logging)
- [ ] Index created for performance

---

## 🚀 SUBMIT YOUR WORK

### Step 1: Verify changes
```bash
git status
git diff
```

### Step 2: Commit
```bash
git add src/main/resources/db/migration/V2__add_booking_seat_denormalized_fields.sql
git add src/main/java/com/trainning/movie_booking_system/entity/BookingSeat.java
git add src/main/java/com/trainning/movie_booking_system/dto/response/Booking/BookingSeatDTO.java
git add src/main/java/com/trainning/movie_booking_system/service/impl/BookingServiceImpl.java

git commit -m "fix: Add denormalized seat info to BookingSeat for better performance

WHAT CHANGED:
- Added seat_number, row_label, seat_type columns to booking_seats table
- Created database migration V2 with data migration
- Updated BookingSeat entity with 3 new fields
- Updated BookingSeatDTO with display fields + computed seatLabel
- Service now copies seat info when creating booking
- Added index on (row_label, seat_number) for performance

WHY:
- Frontend couldn't display seat info without extra queries (N+1 problem)
- API response was incomplete
- Poor user experience showing seat details
- Performance issue with multiple joins

HOW IT WORKS:
- Denormalize seat display data into booking_seats table
- Copy seatNumber, rowLabel, seatType from Seat entity
- Computed seatLabel property for easy display (e.g., 'A1')
- Existing data migrated via UPDATE query

PERFORMANCE IMPACT:
- Eliminates N+1 query problem
- No join needed to display seat info
- Index added for fast lookups

TESTING:
- Database migration successful
- Existing data migrated correctly
- API returns complete seat information
- All tests pass

Fixes: BUG #2 - BookingSeat missing rowLabel
Related: BUGS_TO_FIX.md"
```

### Step 3: Push
```bash
git push origin bugfix/booking-seat-denormalized-fields
```

---

## 💡 IMPORTANT NOTES

### ⚠️ Database Migration Tips

1. **DEFAULT values:** Chỉ dùng để migrate data, sau đó DROP DEFAULT
2. **Data integrity:** Update existing data BEFORE dropping defaults
3. **Rollback plan:** Backup database trước khi migrate
4. **Test locally:** Chạy migration trên local DB trước

### ⚠️ Code Tips

1. **Copy from Seat entity:** Đừng hardcode values
2. **Null safety:** Seat entity có thể null, cần check
3. **Index:** Thêm index để improve query performance
4. **Computed field:** `seatLabel` = rowLabel + seatNumber

### ⚠️ Testing Tips

1. **Verify migration:** Check existing data có migrate đúng
2. **Check N+1:** Enable SQL logging, verify không có extra queries
3. **API response:** Verify có đầy đủ seat info
4. **Performance:** Compare query time trước/sau

---

## 📞 NEED HELP?

**Questions:**
1. Làm sao tạo migration file? → `src/main/resources/db/migration/V2__*.sql`
2. Flyway không chạy? → Check `application.yml` config
3. Data không migrate? → Check FK constraints, run UPDATE manual
4. Test lỗi? → Enable SQL logging, check entity mapping

**Stuck?** Hỏi tôi! 🤝

---

## ⏱️ TIME ESTIMATE

| Task | Time |
|------|------|
| Create migration SQL | 15 min |
| Run migration + verify | 10 min |
| Update Entity | 10 min |
| Update DTO | 5 min |
| Update Service | 15 min |
| Testing | 20 min |
| **Total** | **1h 15min** |

---

**Ready?** Bắt đầu với migration script! 🚀
