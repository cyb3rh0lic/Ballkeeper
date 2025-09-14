package capstone.ballkeeper.event;

import capstone.ballkeeper.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
// import org.springframework.scheduling.annotation.Async; // 비동기 원하면 주석 해제

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final NotificationService notificationService;

    // @Async // 비동기로 돌리고 싶으면 활성화(그리고 @EnableAsync 필요)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event) {
        log.info("[EVENT] Reservation created: reservationId={}", event.reservationId());
        try {
            notificationService.notifyAdminsOnReservationCreated(event.reservationId());
            // 필요하면 여기서 웹소켓/슬랙/웹훅 등 추가
        } catch (Exception e) {
            // 본 트랜잭션에 영향 주지 않도록 경고만
            log.warn("Failed to notify admins for reservationId={}", event.reservationId(), e);
        }
    }
}

