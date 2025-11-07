# API Usage Example - User Information

## 1. Đăng ký Account với thông tin User

**Endpoint:** `POST /api/accounts/register`

**Request Body:**
```json
{
  "username": "john_doe",
  "email": "john.doe@example.com",
  "password": "password123",
  "fullName": "John Doe",
  "phone": "0123456789",
  "dateOfBirth": "1990-05-15",
  "gender": "MALE",
  "address": "123 Main Street",
  "city": "Ho Chi Minh City"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Registered successfully",
  "result": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": null,
    "tokenType": "Bearer",
    "expiresIn": 300,
    "accountId": 1,
    "username": "john_doe",
    "email": "john.doe@example.com",
    "emailVerified": false,
    "status": "ACTIVE",
    "user": {
      "id": 1,
      "accountId": 1,
      "fullName": "John Doe",
      "phone": "0123456789",
      "dateOfBirth": "1990-05-15",
      "gender": "MALE",
      "avatarUrl": null,
      "address": "123 Main Street",
      "city": "Ho Chi Minh City",
      "createdAt": "2024-01-15T10:30:00",
      "updatedAt": "2024-01-15T10:30:00"
    }
  }
}
```

## 2. Đăng nhập

**Endpoint:** `POST /api/accounts/login`

**Request Body:**
```json
{
  "usernameOrEmail": "john_doe",
  "password": "password123"
}
```

**Response:** (Tương tự như register, bao gồm thông tin user)

## 3. Xem thông tin profile

**Endpoint:** `GET /api/users/profile`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "success": true,
  "message": "User profile retrieved successfully",
  "result": {
    "id": 1,
    "accountId": 1,
    "fullName": "John Doe",
    "phone": "0123456789",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "avatarUrl": null,
    "address": "123 Main Street",
    "city": "Ho Chi Minh City",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

## 4. Cập nhật thông tin profile

**Endpoint:** `PUT /api/users/profile`

**Headers:**
```
Authorization: Bearer <access_token>
```

**Request Body:**
```json
{
  "fullName": "John Smith",
  "phone": "0987654321",
  "address": "456 New Street",
  "city": "Hanoi",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

**Response:**
```json
{
  "success": true,
  "message": "User profile updated successfully",
  "result": {
    "id": 1,
    "accountId": 1,
    "fullName": "John Smith",
    "phone": "0987654321",
    "dateOfBirth": "1990-05-15",
    "gender": "MALE",
    "avatarUrl": "https://example.com/avatar.jpg",
    "address": "456 New Street",
    "city": "Hanoi",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T11:45:00"
  }
}
```

## Các trường thông tin User:

- **fullName**: Tên đầy đủ (bắt buộc, 2-100 ký tự)
- **phone**: Số điện thoại (bắt buộc, 10-15 ký tự)
- **dateOfBirth**: Ngày sinh (tùy chọn)
- **gender**: Giới tính (tùy chọn, MALE/FEMALE/OTHER)
- **address**: Địa chỉ (tùy chọn)
- **city**: Thành phố (tùy chọn)
- **avatarUrl**: URL ảnh đại diện (tùy chọn)
