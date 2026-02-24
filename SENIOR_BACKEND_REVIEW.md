# Senior Backend Review - Backend Movie Booking System

## 1) Executive Summary

Hệ thống đã đi đúng hướng của một backend production-ready ở các trục chính: kiến trúc layered rõ ràng, JWT + RBAC, flow giữ ghế bằng Redis lock, và tách domain khá sạch theo module (auth, movie, showtime, booking, payment, voucher).

Tuy nhiên, theo chuẩn senior backend để đưa production ổn định, hiện còn một số điểm cần ưu tiên xử lý ngay: nhất quán authorization role, chuẩn hóa trạng thái expire booking (đang có 2 scheduler + logic khác nhau), và hoàn thiện idempotency/payment consistency ở các luồng callback/IPN.

**Đánh giá tổng quan hiện tại:**
- Kiến trúc & tổ chức code: **B+**
- Correctness nghiệp vụ booking/payment: **B**
- Bảo mật & authorization consistency: **B-**
- Observability & operations readiness: **C+**
- Testability: **C**

---

## 2) Feature Review (đã implement)

## 2.1 Authentication & User

### Điểm mạnh
- Có đầy đủ luồng register/activate/login/refresh/logout/forgot/reset trong API auth v1.
- Security config dùng stateless + JWT filter + method security, phù hợp backend API.

### Nhận xét senior
- Phần nền tảng auth đã đủ để chạy production ở mức cơ bản.
- Cần thêm chuẩn hóa audit log security event (login failed/success, refresh token revoke) và hardening rate-limit ngoài OTP.

## 2.2 Movie / Theater / Screen / Showtime

### Điểm mạnh
- CRUD cho domain vận hành rạp/phim/suất chiếu đã có và phân lớp controller-service-repository rõ ràng.
- Có search và count endpoint, phù hợp nhu cầu UI dashboard.

### Nhận xét senior
- Nên chuẩn hóa policy role theo 1 naming convention duy nhất (ROLE_ADMIN, ROLE_THEATER_MANAGEMENT, ...), tránh sai lệch giữa enum role và `@PreAuthorize`.

## 2.3 Seat Hold + Booking

### Điểm mạnh
- Flow giữ ghế có xác minh hold-by-user trước và sau lock, chống race condition tốt hơn mức cơ bản.
- Có lock seat theo thứ tự ID để giảm deadlock phân tán.
- Booking seat đã có lưu thông tin denormalized (`seatNumber`, `rowLabel`, `seatType`) giúp giảm query phụ cho frontend.

### Nhận xét senior
- Có validate cutoff showtime 15 phút trước giờ chiếu trong service booking, đây là điểm rất tốt.
- Tuy nhiên API contract nên bỏ hẳn các endpoint update/delete booking nếu business không hỗ trợ (tránh anti-pattern public API nhưng always throw unsupported).

## 2.4 Payment (VNPay integration level)

### Điểm mạnh
- Có cấu trúc tách `PaymentTransaction`, callback/verify/IPN endpoints, và có verify signature trước xử lý callback.
- Có idempotency mức cơ bản ở transaction status SUCCESS/FAILED.

### Nhận xét senior
- Hiện tồn tại **2 cơ chế expire booking** khác nhau (PaymentService scheduler và BookingExpireService), dễ gây trạng thái không nhất quán.
- `createPaymentUrl` dùng `finalAmount` nhưng không có guard cứng khi giá trị null (phụ thuộc dữ liệu lịch sử/bug cũ).
- Cần policy idempotency chặt hơn theo transactionId + gatewayOrderId + unique constraint.

## 2.5 Voucher

### Điểm mạnh
- Voucher có validate, usage, admin CRUD, và job cập nhật trạng thái voucher theo lịch.
- Tích hợp voucher trực tiếp vào booking flow (validate trước khi finalize amount).

### Nhận xét senior
- Nên có transaction boundary rõ ràng quanh toàn bộ flow apply voucher + tạo booking để tránh trạng thái nửa vời khi exception giữa chừng.

---

## 3) Findings quan trọng cần fix

## CRITICAL
1. **Authorization role naming không nhất quán**
   - `SeatController` đang dùng `hasAnyRole('ADMIN', 'MANAGER')` nhưng enum role hiện có `STAFF` và `THEATER_MANAGEMENT`, không có `MANAGER`.
   - Hệ quả: quyền thao tác seat có thể bị deny sai hoặc phát sinh lỗ hổng do mapping role không rõ ràng.

2. **Hai scheduler xử lý expire booking đang chồng chéo**
   - `BookingExpireService` expire theo repository query.
   - `PaymentServiceImpl.releaseExpiredBookings` cũng tự expire theo `PaymentTransaction.initiatedAt`.
   - Hệ quả: cạnh tranh cập nhật state, khó debug incident, và dễ phát sinh bug release seat không đồng nhất.

3. **Repository query đặt tên expires nhưng query theo `bookingDate`**
   - `findAllExpiredBookings` comment mô tả theo expiry, nhưng điều kiện query dùng `bookingDate < now`.
   - Hệ quả: semantics sai, dễ gây expire sai trong các case booking có `expiresAt` custom.

## MAJOR
4. **Public API có endpoint unsupported**
   - Booking update/delete exposed nhưng service throw `UnsupportedOperationException`.
   - Khuyến nghị: xóa endpoint hoặc đổi sang luồng nghiệp vụ cancel rõ ràng.

5. **Test endpoint chưa bảo vệ đúng theo tên endpoint**
   - `/api/protected/admin-only` chưa có `@PreAuthorize` tương ứng.
   - Khuyến nghị: nếu giữ endpoint, phải enforce role admin đúng nghĩa.

6. **Operational observability còn mỏng**
   - Thiếu correlation-id xuyên suốt booking → payment → callback.
   - Thiếu metric business (hold conflict rate, payment success rate, expired booking rate).

---

## 4) Khuyến nghị chuẩn senior (ưu tiên triển khai)

## P0 - trong 1-2 ngày
- Chuẩn hóa toàn bộ role expression (`hasRole/hasAuthority`) + map role enum thống nhất.
- Chỉ giữ **1** nguồn sự thật cho expire booking (ưu tiên `expiresAt`), loại bỏ scheduler trùng lặp.
- Sửa repository query và đặt tên method đúng semantics (`findAllByStatusAndExpiresAtBefore`).
- Đóng hoặc refactor endpoint booking update/delete.

## P1 - trong 1 tuần
- Bổ sung integration tests cho 4 luồng critical:
  1) concurrent hold cùng ghế,
  2) create booking sát cutoff showtime,
  3) callback payment idempotent gọi lặp,
  4) expire booking và release hold đúng.
- Bổ sung logging chuẩn JSON + correlation-id (MDC).
- Thêm unique/index ràng buộc transaction theo gateway.

## P2 - trong 2-4 tuần
- Tách rõ payment orchestration thành state machine hoặc saga nhẹ.
- Hardening resilience: retry policy có backoff, dead-letter cho webhook fail.
- Bổ sung dashboard vận hành + alert (error rate, callback latency, DB lock wait).

---

## 5) Kết luận

Codebase có nền tảng tốt và nhiều phần đã vượt mức CRUD thông thường (đặc biệt ở booking concurrency). Để đạt chuẩn senior backend production, trọng tâm không còn ở "thêm tính năng" mà ở **nhất quán trạng thái**, **độ tin cậy payment**, và **khả năng vận hành/quan sát khi có sự cố**. Nếu hoàn thành các hạng mục P0 + P1 ở trên, hệ thống sẽ tăng mạnh độ an toàn khi scale traffic thực tế.
