package capstone.ballkeeper.service;

import capstone.ballkeeper.domain.item.Item;
import capstone.ballkeeper.domain.member.Member;
import capstone.ballkeeper.domain.member.Role;
import capstone.ballkeeper.domain.notification.Notification;
import capstone.ballkeeper.domain.notification.NotificationType;
import capstone.ballkeeper.domain.reservation.Reservation;
import capstone.ballkeeper.dto.NotificationPayload;
import capstone.ballkeeper.infra.sse.SseEmitterRegistry;
import capstone.ballkeeper.repository.MemberRepository;
import capstone.ballkeeper.repository.NotificationRepository;
import capstone.ballkeeper.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final SseEmitterRegistry registry;

    /** 1) 구독 (최소 구현: 인증 없이 memberId 쿼리로 받기) */
    public SseEmitter subscribe(Long memberId) {
        SseEmitter emitter = new SseEmitter(0L); // 사실상 무제한
        registry.add(memberId, emitter);

        emitter.onCompletion(() -> registry.remove(memberId, emitter));
        emitter.onTimeout(() -> registry.remove(memberId, emitter));
        emitter.onError(ex -> registry.remove(memberId, emitter));

        // 첫 핑으로 연결 확인
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /* ===== 내부 유틸: 실제 SSE 전송 ===== */
    private void sendTo(Long memberId, Long eventId, String eventName, Object data) {
        var emitters = registry.get(memberId);
        for (SseEmitter em : emitters) {
            try {
                em.send(SseEmitter.event()
                        .id(String.valueOf(eventId))
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.warn("SSE send fail to memberId={}", memberId, e);
                em.completeWithError(e);
            }
        }
    }

    /* ===== 관리자에게 예약 생성 알림(핵심 요구사항) ===== */
    @Transactional
    public void notifyAdminsOnReservationCreated(Long reservationId) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) {
            throw new IllegalArgumentException("예약 없음: " + reservationId);
        }

        Member user = r.getMember();
        Item item = r.getReservationItems().isEmpty() ? null : r.getReservationItems().get(0).getItem();
        Long itemId = (item != null ? item.getId() : null);
        String photoUrl = (item != null ? item.getImageUrl() : null); // Item에 getImageUrl 없으면 null 처리

        String message = (user != null ? user.getName() : "사용자") +
                "님이 예약을 생성했습니다.";

        // 1) DB에 알림 저장 + 2) 연결된 ADMIN에게만 SSE 전송
        List<Member> admins = memberRepository.findByRole(Role.ADMIN);
        for (Member admin : admins) {
            Notification n = Notification.createNotification(r, admin, item, NotificationType.RESERVATION_CREATED, message, photoUrl);
            notificationRepository.save(n);

            NotificationPayload payload = NotificationPayload.builder()
                    .type("RESERVATION_CREATED")
                    .reservationId(r.getId())
                    .userId(user != null ? user.getId() : null)
                    .itemId(itemId)
                    .photoUrl(photoUrl)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            sendTo(admin.getId(), n.getId(), "reservation-created", payload);
        }
    }
}
