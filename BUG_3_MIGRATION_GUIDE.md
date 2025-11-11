# 🔧 BUG #3 - DATABASE MIGRATION GUIDE

## ⚠️ IMPORTANT: Manual Migration Required

Sau khi deploy code BUG #3, cần chạy migration SQL để update data cũ.

---

## 📋 MIGRATION STEPS

### Step 1: Check Existing Data

```sql
-- Check bookings without expiresAt
SELECT 
    id, 
    booking_date, 
    expires_at, 
    status 
FROM bookings 
WHERE expires_at IS NULL
LIMIT 10;
```

### Step 2: Update Existing Bookings

```sql
-- Option 1: Set all NULL bookings to EXPIRED (SAFEST)
-- Recommended for old bookings
UPDATE bookings 
SET 
    expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE),
    status = 'EXPIRED'
WHERE expires_at IS NULL 
  AND status = 'PENDING_PAYMENT';

-- Option 2: Just set expiresAt (keep status as is)
-- Use if you want to preserve current status
UPDATE bookings 
SET expires_at = DATE_ADD(booking_date, INTERVAL 15 MINUTE)
WHERE expires_at IS NULL;
```

### Step 3: Verify Migration

```sql
-- Verify no NULL values remain
SELECT COUNT(*) FROM bookings WHERE expires_at IS NULL;
-- Expected: 0

-- Check updated records
SELECT 
    id,
    booking_date,
    expires_at,
    TIMESTAMPDIFF(MINUTE, booking_date, expires_at) as minutes_diff,
    status
FROM bookings
ORDER BY id DESC
LIMIT 20;
-- Expected: minutes_diff = 15 for all
```

### Step 4 (Optional): Make Column NOT NULL

```sql
-- After confirming all data migrated successfully
-- Make column NOT NULL for data integrity
ALTER TABLE bookings 
MODIFY COLUMN expires_at DATETIME NOT NULL;
```

---

## 🧪 TESTING AFTER MIGRATION

### Test 1: Create New Booking
```bash
POST /api/bookings
{
  "showtimeId": 1,
  "seatIds": [1, 2]
}

# Check database
SELECT id, booking_date, expires_at FROM bookings ORDER BY id DESC LIMIT 1;
# Should have expiresAt = bookingDate + 15 min
```

### Test 2: Cron Job
```sql
-- Create expired booking for testing
INSERT INTO bookings (account_id, showtime_id, total_price, status, booking_date, expires_at, created_at, updated_at)
VALUES (1, 1, 300000, 'PENDING_PAYMENT', 
        NOW() - INTERVAL 20 MINUTE,  -- 20 minutes ago
        NOW() - INTERVAL 5 MINUTE,   -- expired 5 minutes ago
        NOW(), NOW());

-- Wait for cron job (runs every 5 minutes)
-- Or trigger manually in code

-- Check status changed to EXPIRED
SELECT id, status, expires_at FROM bookings WHERE id = LAST_INSERT_ID();
```

---

## 📊 ROLLBACK PLAN (If needed)

```sql
-- If migration causes issues, rollback:

-- Option 1: Drop column
ALTER TABLE bookings DROP COLUMN expires_at;

-- Option 2: Set all to NULL
UPDATE bookings SET expires_at = NULL;
```

---

## ✅ CHECKLIST

- [ ] Backup database before migration
- [ ] Run Step 1: Check existing data
- [ ] Run Step 2: Update bookings (choose Option 1 or 2)
- [ ] Run Step 3: Verify migration
- [ ] Test new booking creation
- [ ] Test cron job expiration
- [ ] (Optional) Run Step 4: Make NOT NULL
- [ ] Monitor logs for errors

---

## 💡 NOTES

**Why nullable?**
- Allows safe migration of existing data
- JPA can create column without errors
- Prevents data truncation errors

**When to make NOT NULL?**
- After all existing data migrated
- After testing in production
- Recommended: Keep nullable for a few days, then make NOT NULL

**Cron Job:**
- Still works with NULL values (query uses `WHERE expiresAt < NOW()`)
- NULL bookings simply won't be selected by cron
- After migration, all bookings will have expiresAt

---

Generated: 2025-11-11
Bug: #3 - Booking Expiration
