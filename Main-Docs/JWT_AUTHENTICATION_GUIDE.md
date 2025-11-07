# JWT Authentication Guide

## Tổng quan
Hệ thống đã được tích hợp JWT (JSON Web Token) authentication với các tính năng sau:

### 🔑 Tính năng chính:
- **Access Token**: Token ngắn hạn (5 phút) để xác thực các API calls
- **Refresh Token**: Token dài hạn (14 ngày) để làm mới access token
- **Token Validation**: Kiểm tra tính hợp lệ của token
- **Auto Token Refresh**: Tự động tạo token mới khi cần

## 📋 API Endpoints

### 1. Đăng ký (tự động tạo token)
```http
POST /api/accounts/register
Content-Type: application/json

{
    "username": "john_doe",
    "email": "john@example.com",
    "password": "password123"
}
```

**Response:**
```json
{
    "success": true,
    "message": "Registered successfully",
    "result": {
        "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "tokenType": "Bearer",
        "expiresIn": 300,
        "accountId": 1,
        "username": "john_doe",
        "email": "john@example.com",
        "emailVerified": false,
        "status": "ACTIVE"
    }
}
```

### 2. Đăng nhập (tự động tạo token)
```http
POST /api/accounts/login
Content-Type: application/json

{
    "usernameOrEmail": "john_doe",
    "password": "password123"
}
```

**Response:** Tương tự như đăng ký

## 🔧 Cấu hình JWT

### application.yml
```yaml
jwt:
  expiryMinutes: 5          # Thời gian sống của access token (phút)
  expiryDay: 14             # Thời gian sống của refresh token (ngày)
  accessKey: p1zs6HE4bebUL6aVjJhSEtj/Bp4rhQPK3vUR2gpcA06muNhYO77Z9kzb3U5sDXBkdpEZwECJfm0tNuDRiVu29g==
```

## 🔄 Cách sử dụng JWT Token

### 1. **Đăng ký/Đăng nhập để lấy token**
```javascript
// User đăng ký hoặc đăng nhập
const response = await fetch('/api/accounts/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
        usernameOrEmail: 'john_doe',
        password: 'password123'
    })
});

const { result } = await response.json();

// Lưu token để sử dụng cho các request tiếp theo
localStorage.setItem('accessToken', result.accessToken);
```

### 2. **Sử dụng token cho phân quyền**
```javascript
// Sử dụng token trong các API calls để xác thực quyền
const token = localStorage.getItem('accessToken');
const response = await fetch('/api/admin/users', {
    headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    }
});
```

### 3. **Decode token để lấy thông tin user**
```javascript
// Decode JWT token để lấy thông tin user
function decodeToken(token) {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return {
            accountId: payload.accountId,
            username: payload.username,
            email: payload.email,
            status: payload.status,
            emailVerified: payload.emailVerified
        };
    } catch (error) {
        console.error('Invalid token:', error);
        return null;
    }
}

// Sử dụng
const token = localStorage.getItem('accessToken');
const userInfo = decodeToken(token);
console.log('User:', userInfo.username);
console.log('Status:', userInfo.status);
```

### 4. **Kiểm tra quyền dựa trên token**
```javascript
// Kiểm tra user có phải admin không
function isAdmin(token) {
    const userInfo = decodeToken(token);
    return userInfo && userInfo.status === 'ADMIN';
}

// Kiểm tra user có active không
function isActive(token) {
    const userInfo = decodeToken(token);
    return userInfo && userInfo.status === 'ACTIVE';
}

// Sử dụng
const token = localStorage.getItem('accessToken');
if (isAdmin(token)) {
    // Hiển thị menu admin
    showAdminMenu();
} else {
    // Ẩn menu admin
    hideAdminMenu();
}
```

### 5. **Xử lý khi token hết hạn**
```javascript
// Khi nhận được 401 Unauthorized
if (response.status === 401) {
    // Token hết hạn, cần đăng nhập lại
    localStorage.removeItem('accessToken');
    // Redirect to login page
    window.location.href = '/login';
}
```

## 🔒 Bảo mật

### 1. Token Security
- Access token có thời gian sống ngắn (5 phút)
- Refresh token có thời gian sống dài hơn (14 ngày)
- Sử dụng HMAC SHA-256 để ký token
- Secret key được lưu trữ an toàn trong application.yml

### 2. Best Practices
- Luôn sử dụng HTTPS trong production
- Không lưu token trong URL parameters
- Implement token rotation (refresh token cũ bị vô hiệu hóa khi tạo mới)
- Logout khi refresh token hết hạn

## 🚀 Lợi ích

1. **Stateless Authentication**: Không cần lưu session trên server
2. **Scalable**: Dễ dàng scale horizontal
3. **Secure**: Token được ký và mã hóa
4. **Flexible**: Có thể chứa thông tin user trong token
5. **Cross-Domain**: Hoạt động tốt với CORS

## 📝 Lưu ý

- Access token hết hạn sau 5 phút, cần refresh thường xuyên
- Refresh token hết hạn sau 14 ngày, cần đăng nhập lại
- Token chứa username, không chứa password
- Có thể customize thời gian sống token trong application.yml
