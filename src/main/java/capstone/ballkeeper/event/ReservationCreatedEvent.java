package capstone.ballkeeper.event;

/**
 * 예약이 성공적으로 생성되었음을 알리는 도메인 이벤트.
 * 트랜잭션 커밋 후 리스너가 받아서 별도 작업(알림/로그/브로드캐스트 등)을 수행할 수 있다.
 */
public record ReservationCreatedEvent(Long reservationId) {}
