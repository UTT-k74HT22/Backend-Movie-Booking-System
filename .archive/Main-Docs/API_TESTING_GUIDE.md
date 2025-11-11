# Movie Booking System API Test Guide

## Tổng quan
Bộ test collection này cung cấp các test case hoàn chỉnh cho tất cả API endpoints của hệ thống Movie Booking System, bao gồm:

- **Authentication** - Xác thực người dùng
- **Movies** - Quản lý phim
- **Theaters** - Quản lý rạp chiếu phim
- **Screens** - Quản lý màn hình/phòng chiếu
- **Showtimes** - Quản lý suất chiếu
- **OTP** - Quản lý mã OTP
- **Protected Endpoints** - Các endpoint được bảo vệ
- **Negative Test Cases** - Các test case kiểm tra lỗi

## Cài đặt và Sử dụng

### 1. Import Collection và Environment
1. Mở Postman
2. Click **Import** 
3. Chọn file `Movie_Booking_System_API_Tests.postman_collection.json`
4. Import environment file `Movie_Booking_System_Environment.postman_environment.json`
5. Chọn environment "Movie Booking System Environment"

### 2. Thiết lập Environment Variables
Đảm bảo các biến sau được thiết lập trong environment:
- `baseUrl`: http://localhost:8080 (hoặc URL server của bạn)
- `accessToken`: Sẽ được tự động cập nhật sau khi login
- `refreshToken`: Sẽ được tự động cập nhật sau khi login
- `movieId`, `theaterId`, `screenId`, `showtimeId`: Sẽ được tự động cập nhật khi tạo resources

## Chi tiết Test Cases

### Authentication APIs

#### 1. Register User
- **Method:** POST
- **URL:** `/api/auth/register`
- **Body:**
```json
{
    "username": "testuser{{$randomInt}}",
    "email": "testuser{{$randomInt}}@example.com",
    "password": "Password123!",
    "firstName": "Test",
    "lastName": "User",
    "phone": "0987654321"
}
```
- **Tests:**
  - Status code 200
  - Response success = true
  - Response time < 2000ms

#### 2. Login User
- **Method:** POST
- **URL:** `/api/auth/login`
- **Body:**
```json
{
    "username": "admin",
    "password": "admin123"
}
```
- **Tests:**
  - Status code 200
  - Response contains accessToken và refreshToken
  - Tự động lưu tokens vào environment variables
  - Response time < 2000ms

#### 3. Test Authentication (Me)
- **Method:** GET
- **URL:** `/api/auth/me`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Tests:**
  - Status code 200
  - Authentication successful

#### 4. Refresh Token
- **Method:** POST
- **URL:** `/api/auth/refresh-token`
- **Body:**
```json
{
    "refreshToken": "{{refreshToken}}"
}
```
- **Tests:**
  - Status code 200
  - Response contains new accessToken
  - Tự động cập nhật accessToken

#### 5. Forgot Password
- **Method:** POST
- **URL:** `/api/auth/forgot-password`
- **Body:**
```json
{
    "email": "test@example.com"
}
```

#### 6. Activate Account
- **Method:** POST
- **URL:** `/api/auth/activate`
- **Body:**
```json
{
    "email": "test@example.com",
    "otp": "123456"
}
```

#### 7. Reset Password
- **Method:** POST
- **URL:** `/api/auth/reset-password`
- **Body:**
```json
{
    "email": "test@example.com",
    "otp": "123456",
    "newPassword": "NewPassword123!"
}
```

#### 8. Logout
- **Method:** POST
- **URL:** `/api/auth/logout`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "refreshToken": "{{refreshToken}}"
}
```

### Movies APIs

#### 1. Create Movie
- **Method:** POST
- **URL:** `/api/movies`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "title": "Test Movie {{$randomInt}}",
    "description": "This is a test movie description for testing purposes.",
    "duration": 120,
    "language": "Vietnamese",
    "country": "Vietnam",
    "releaseDate": "2024-01-01",
    "rating": 4.5,
    "genres": ["Action", "Comedy"],
    "director": "Test Director",
    "actors": ["Actor 1", "Actor 2"],
    "posterUrl": "https://example.com/poster.jpg",
    "trailerUrl": "https://example.com/trailer.mp4",
    "status": "ACTIVE"
}
```
- **Tests:**
  - Status code 200
  - Movie created successfully
  - Tự động lưu movieId
  - Response có các properties bắt buộc

#### 2. Get All Movies
- **Method:** GET
- **URL:** `/api/movies?pageNumber=0&pageSize=10`
- **Tests:**
  - Status code 200
  - Response có pagination data
  - Content là array

#### 3. Get Movie by ID
- **Method:** GET
- **URL:** `/api/movies/{{movieId}}`
- **Tests:**
  - Status code 200
  - Response có id và title

#### 4. Search Movies
- **Method:** GET
- **URL:** `/api/movies/search?keyword=test&page=0&size=10&sortBy=releaseDate&direction=DESC`
- **Tests:**
  - Status code 200
  - Search results returned

#### 5. Search Movies with Filters
- **Method:** GET
- **URL:** `/api/movies/search?genres=Action,Comedy&language=Vietnamese&status=ACTIVE&ratingMin=3.0&ratingMax=5.0&durationMin=90&durationMax=180`

#### 6. Count Total Movies
- **Method:** GET
- **URL:** `/api/movies/count`
- **Tests:**
  - Status code 200
  - Response là number

#### 7. Update Movie
- **Method:** PATCH
- **URL:** `/api/movies/{{movieId}}`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "title": "Updated Test Movie",
    "description": "Updated description for the test movie.",
    "rating": 4.8
}
```

#### 8. Delete Movie
- **Method:** DELETE
- **URL:** `/api/movies/{{movieId}}?movieStatus=INACTIVE`
- **Headers:** Authorization: Bearer {{accessToken}}

### Theaters APIs

#### 1. Create Theater
- **Method:** POST
- **URL:** `/api/theaters`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "name": "Test Theater {{$randomInt}}",
    "address": "123 Test Street, Test City",
    "phone": "0987654321",
    "email": "theater{{$randomInt}}@example.com",
    "description": "A test theater for testing purposes",
    "status": "ACTIVE"
}
```
- **Tests:**
  - Status code 200
  - Theater created successfully
  - Tự động lưu theaterId

#### 2. Get All Theaters
- **Method:** GET
- **URL:** `/api/theaters?pageNumber=0&pageSize=10`

#### 3. Get Theater by ID
- **Method:** GET
- **URL:** `/api/theaters/{{theaterId}}`

#### 4. Get Movies by Theater
- **Method:** GET
- **URL:** `/api/theaters/{{theaterId}}/movies`

#### 5. Get Movies by Theater with Date
- **Method:** GET
- **URL:** `/api/theaters/{{theaterId}}/movies?date=2024-01-15`

#### 6. Count Theaters
- **Method:** GET
- **URL:** `/api/theaters/count`

#### 7. Update Theater
- **Method:** PATCH
- **URL:** `/api/theaters/{{theaterId}}`
- **Headers:** Authorization: Bearer {{accessToken}}

#### 8. Delete Theater
- **Method:** DELETE
- **URL:** `/api/theaters/{{theaterId}}?status=INACTIVE`
- **Headers:** Authorization: Bearer {{accessToken}}

### Screens APIs

#### 1. Create Screen
- **Method:** POST
- **URL:** `/api/screens`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "name": "Screen {{$randomInt}}",
    "theaterId": {{theaterId}},
    "capacity": 100,
    "description": "Test screen for testing purposes",
    "status": "ACTIVE"
}
```

#### 2. Get All Screens
- **Method:** GET
- **URL:** `/api/screens?pageNumber=0&pageSize=10`

#### 3. Get Screen by ID
- **Method:** GET
- **URL:** `/api/screens/{{screenId}}`

#### 4. Get Screens by Theater
- **Method:** GET
- **URL:** `/api/screens/theater/{{theaterId}}`

#### 5. Count All Screens
- **Method:** GET
- **URL:** `/api/screens/count`

#### 6. Update Screen
- **Method:** PATCH
- **URL:** `/api/screens/{{screenId}}`
- **Headers:** Authorization: Bearer {{accessToken}}

#### 7. Delete Screen
- **Method:** DELETE
- **URL:** `/api/screens/{{screenId}}?status=INACTIVE`
- **Headers:** Authorization: Bearer {{accessToken}}

### Showtimes APIs

#### 1. Create Showtime
- **Method:** POST
- **URL:** `/api/showtimes`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "movieId": {{movieId}},
    "screenId": {{screenId}},
    "startTime": "2024-01-15T20:00:00",
    "endTime": "2024-01-15T22:00:00",
    "price": 150000,
    "status": "ACTIVE"
}
```

#### 2. Get All Showtimes
- **Method:** GET
- **URL:** `/api/showtimes?pageNumber=0&pageSize=10`

#### 3. Get Showtime by ID
- **Method:** GET
- **URL:** `/api/showtimes/{{showtimeId}}`

#### 4. Get Showtimes by Theater and Movie
- **Method:** GET
- **URL:** `/api/showtimes/by-theater-and-movie?theaterId={{theaterId}}&movieId={{movieId}}&date=2024-01-15`

#### 5. Count Showtimes
- **Method:** GET
- **URL:** `/api/showtimes/count`

#### 6. Update Showtime
- **Method:** PATCH
- **URL:** `/api/showtimes/{{showtimeId}}`
- **Headers:** Authorization: Bearer {{accessToken}}

#### 7. Delete Showtime
- **Method:** DELETE
- **URL:** `/api/showtimes/{{showtimeId}}?status=INACTIVE`
- **Headers:** Authorization: Bearer {{accessToken}}

### OTP APIs

#### 1. Resend OTP
- **Method:** POST
- **URL:** `/api/otp/resend`
- **Body:**
```json
{
    "email": "test@example.com",
    "type": "ACTIVATION"
}
```

#### 2. Resend OTP for Password Reset
- **Method:** POST
- **URL:** `/api/otp/resend`
- **Body:**
```json
{
    "email": "test@example.com",
    "type": "PASSWORD_RESET"
}
```

### Protected Endpoints

#### 1. Get User Info (Admin Only)
- **Method:** GET
- **URL:** `/api/protected/user-info`
- **Headers:** Authorization: Bearer {{accessToken}}

#### 2. Admin Only Endpoint
- **Method:** GET
- **URL:** `/api/protected/admin-only`
- **Headers:** Authorization: Bearer {{accessToken}}

### Negative Test Cases

#### 1. Login with Invalid Credentials
- **Method:** POST
- **URL:** `/api/auth/login`
- **Body:**
```json
{
    "username": "invaliduser",
    "password": "wrongpassword"
}
```
- **Expected:** Status 400 or 401

#### 2. Access Protected Endpoint Without Token
- **Method:** GET
- **URL:** `/api/protected/user-info`
- **Expected:** Status 401

#### 3. Create Movie Without Authorization
- **Method:** POST
- **URL:** `/api/movies`
- **Expected:** Status 401

#### 4. Get Non-existent Movie
- **Method:** GET
- **URL:** `/api/movies/99999`
- **Expected:** Status 404

#### 5. Create Movie with Invalid Data
- **Method:** POST
- **URL:** `/api/movies`
- **Headers:** Authorization: Bearer {{accessToken}}
- **Body:**
```json
{
    "title": "",
    "duration": -1
}
```
- **Expected:** Status 400

## Thứ tự chạy Test đề xuất

1. **Authentication:**
   - Login User (để lấy tokens)
   - Test Authentication (Me)

2. **Resource Creation:**
   - Create Theater
   - Create Screen (cần theaterId)
   - Create Movie
   - Create Showtime (cần movieId và screenId)

3. **Read Operations:**
   - Get All Movies
   - Get Movie by ID
   - Search Movies
   - Get All Theaters
   - Get Theater by ID
   - Get All Screens
   - Get All Showtimes

4. **Update Operations:**
   - Update Movie
   - Update Theater
   - Update Screen
   - Update Showtime

5. **Count Operations:**
   - Count Movies
   - Count Theaters
   - Count Screens
   - Count Showtimes

6. **Protected Endpoints:**
   - Get User Info
   - Admin Only Endpoint

7. **OTP Operations:**
   - Resend OTP

8. **Negative Test Cases:**
   - Tất cả negative test cases

9. **Cleanup (optional):**
   - Delete Showtime
   - Delete Screen
   - Delete Movie
   - Delete Theater
   - Logout

## Lưu ý quan trọng

1. **Authentication:** Đa số các API cần token JWT. Hãy chạy Login trước tiên.

2. **Dependencies:** Một số resources phụ thuộc vào nhau:
   - Screen cần Theater
   - Showtime cần Movie và Screen

3. **Auto-generated IDs:** Collection tự động lưu các ID được tạo trong environment variables.

4. **Admin Permissions:** Một số API chỉ cho phép admin hoặc theater management access.

5. **Data Validation:** Các request có validation, đảm bảo data đúng format.

6. **Status Codes:** 
   - 200: Success
   - 400: Bad Request (dữ liệu không hợp lệ)
   - 401: Unauthorized (thiếu hoặc sai token)
   - 403: Forbidden (không đủ quyền)
   - 404: Not Found (resource không tồn tại)

## Troubleshooting

1. **Token expired:** Chạy lại Login hoặc Refresh Token
2. **404 errors:** Đảm bảo resource đã được tạo và ID đúng
3. **401 errors:** Kiểm tra token trong environment variables
4. **400 errors:** Kiểm tra format và validation của request body

## Test Automation

Có thể sử dụng Postman Collection Runner để chạy tất cả test cases tự động:

1. Click vào collection name
2. Chọn "Run collection"
3. Chọn environment
4. Chọn các folder/requests muốn chạy
5. Click "Start Run"

Collection này cung cấp test coverage hoàn chỉnh cho tất cả API endpoints của Movie Booking System với các test cases positive và negative, đảm bảo chất lượng API.