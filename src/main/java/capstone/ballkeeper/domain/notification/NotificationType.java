package capstone.ballkeeper.domain.notification;

public enum NotificationType {
    RESERVATION_CREATED,   // 예약 생성 → 관리자에게
    ADMIN_CANCELLED        // 관리자 강제 취소 → 사용자에게
}
