package capstone.ballkeeper.event;

import capstone.ballkeeper.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalApplicationListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 이벤트 핸들러
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventHandler {
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReservationCreated(ReservationCreatedEvent event) {
        try {
            notificationService.notifyAdminsOnReservationCreated(event.reservationId());
        } catch (Exception e) {
            // 알림 실패가 본 트랜잭션에 영향 주지 않도록 로깅
            log.warn("fail to notify admins for reservationId={}", event.reservationId(), e);
        }
    }
}
