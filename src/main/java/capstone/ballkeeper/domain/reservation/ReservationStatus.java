package capstone.ballkeeper.domain.reservation;

public enum ReservationStatus {
    RESERVED,   // 예약 완료 (사용자 요청)
    APPROVED,  // 관리자 승인
    IN_USE,     // 픽업 후 사용 중
    RETURNED,   // 반납 완료
    CANCELLED   // 예약 취소
}
