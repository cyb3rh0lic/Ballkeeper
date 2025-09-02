package capstone.ballkeeper.controller;

import capstone.ballkeeper.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /** 브라우저에서: /api/notifications/connect?memberId={ADMIN_ID} */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect(@RequestParam Long memberId) {
        return ResponseEntity.ok(notificationService.subscribe(memberId));
    }

    /** (임시 테스트) 이미 존재하는 reservationId로 관리자 알림 발행 */
    @PostMapping("/dev/trigger-reservation-created")
    public void devTrigger(@RequestParam Long reservationId) {
        notificationService.notifyAdminsOnReservationCreated(reservationId);
    }
}
