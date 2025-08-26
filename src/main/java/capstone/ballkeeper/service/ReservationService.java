package capstone.ballkeeper.service;

import capstone.ballkeeper.domain.UsageStatus;
import capstone.ballkeeper.domain.item.Item;
import capstone.ballkeeper.domain.member.Member;
import capstone.ballkeeper.domain.reservation.Reservation;
import capstone.ballkeeper.domain.reservation.ReservationStatus;
import capstone.ballkeeper.repository.ItemRepository;
import capstone.ballkeeper.repository.MemberRepository;
import capstone.ballkeeper.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher publisher;

    /**
     * 예약 생성
     */
    @Transactional
    public Long createReservation(Long memberId, Long itemId,
                                  LocalDateTime startTime, LocalDateTime endTime) {

        // 1) 시간 유효성 검증
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작/종료 시각은 필수입니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("시작 시각은 종료 시각보다 앞서야 합니다.");
        }

        // 2) 엔티티 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("물품이 존재하지 않습니다."));

        // 3) 시간대 중복 예약 여부 확인
        if (reservationRepository.existsOverlapping(itemId, startTime, endTime)) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
        }

        // 4) 예약 생성
        Reservation reservation = Reservation.createReservation(
                member,
                startTime,
                endTime,
                ReservationStatus.RESERVED, // 기본 생성 상태
                UsageStatus.PENDING         // 관리자 승인 대기
        );

        // 5) 중간 엔티티 연결 (편의 메서드 사용)
        reservation.addItem(item);

        // 6) 저장
        reservationRepository.save(reservation);


        return reservation.getId();
    }

    // 예약 단건 조회 메서드
    public Reservation findReservation(Long reservationId) {
        return reservationRepository.findOne(reservationId);
    }

    /**
     * 예약 취소
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다.");

        r.changeStatus(ReservationStatus.CANCELLED);

        // 아이템 상태 전환은 보통 픽업/반납 시점에 처리하는 것을 권장
    }

}
