package com.trainning.movie_booking_system.untils.enums;
public enum RoleType {

    /*
    * Giải thích : code : mã định danh role
    *            description : mô tả role
    *           level : cấp độ quyền hạn (số càng cao quyền càng lớn)
    *
    * */
    USER("USER", "Người dùng thông thường", 1),
    STAFF("STAFF", "Nhân viên", 2),
    THEATER_MANAGEMENT("THEATER_MANAGEMENT", "Quản lý rạp chiếu phim", 3),
    ADMIN("ADMIN", "Quản trị viên hệ thống", 4);

    private final String code;
    private final String description;
    private final int level;

    RoleType(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getLevel() {
        return level;
    }

    // Tiện ích: lấy RoleType từ level
    public static RoleType fromLevel(int level) {
        for (RoleType role : RoleType.values()) {
            if (role.getLevel() == level) {
                return role;
            }
        }
        throw new IllegalArgumentException("Không tồn tại role với level: " + level);
    }
}
