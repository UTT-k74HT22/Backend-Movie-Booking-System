# Exception Handling System

Hệ thống xử lý exception đã được hoàn thiện cho Movie Booking System với các thành phần sau:

## 1. ErrorCode Enum
- **Vị trí**: `src/main/java/com/trainning/movie_booking_system/exception/ErrorCode.java`
- **Chức năng**: Định nghĩa tất cả các mã lỗi và thông báo lỗi trong hệ thống
- **Các nhóm lỗi chính**:
  - Account related errors (1001-1007)
  - Authentication & Authorization errors (2001-2004)
  - Role related errors (3001-3003)
  - Permission related errors (4001-4003)
  - Account-Role related errors (5001-5002)
  - Role-Permission related errors (6001-6002)
  - Validation errors (7001-7005)
  - Database errors (8001-8002)
  - External service errors (9001-9002)

## 2. AppException Class
- **Vị trí**: `src/main/java/com/trainning/movie_booking_system/exception/AppException.java`
- **Chức năng**: Custom exception class kế thừa từ RuntimeException
- **Tính năng**:
  - Hỗ trợ ErrorCode
  - Hỗ trợ arguments cho message formatting
  - Hỗ trợ cause exception

## 3. GlobalExceptionHandler
- **Vị trí**: `src/main/java/com/trainning/movie_booking_system/exception/GlobalExceptionHandler.java`
- **Chức năng**: Xử lý tất cả exceptions trong toàn bộ ứng dụng
- **Xử lý các loại exception**:
  - `AppException`: Custom business exceptions
  - `MethodArgumentNotValidException`: Validation errors
  - `IllegalArgumentException`: Invalid arguments
  - `Exception`: Generic exceptions

## 4. Cách sử dụng

### Trong Service Layer:
```java
// Thay vì:
throw new IllegalArgumentException("Username already taken");

// Sử dụng:
throw new AppException(ErrorCode.USERNAME_ALREADY_TAKEN);
```

### Trong Controller Layer:
```java
// Không cần try-catch blocks nữa
@PostMapping("/register")
public ResponseEntity<ApiResponse<AccountResponse>> register(@Valid @RequestBody AccountRegisterRequest request) {
    AccountResponse response = accountService.register(request);
    return ResponseEntity.ok(ApiResponse.<AccountResponse>builder()
            .success(true)
            .message("Registered successfully")
            .result(response)
            .build());
}
```

## 5. Response Format
Tất cả error responses đều có format chuẩn:
```json
{
    "success": false,
    "message": "Error message",
    "result": null
}
```

## 6. Lợi ích
- **Centralized Error Handling**: Tất cả exceptions được xử lý ở một nơi
- **Consistent Error Format**: Format response thống nhất
- **Easy Maintenance**: Dễ dàng thêm/sửa error codes
- **Clean Code**: Controllers không cần try-catch blocks
- **Better Logging**: Logging được xử lý tự động trong GlobalExceptionHandler
