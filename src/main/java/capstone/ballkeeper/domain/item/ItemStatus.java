package capstone.ballkeeper.domain.item;

public enum ItemStatus {
    AVAILABLE,      // 예약 가능
    RESERVED,       // 예약됨 (다른 사람이 예약한 상태)
    IN_USE,         // 사용 중
    NOT_AVAILABLE   // 고장/점검 등으로 사용 불가
}
