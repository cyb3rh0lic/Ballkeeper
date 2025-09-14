package capstone.ballkeeper.service;

import capstone.ballkeeper.domain.UsageStatus;
import capstone.ballkeeper.domain.item.Item;
import capstone.ballkeeper.domain.item.ItemStatus;
import capstone.ballkeeper.domain.member.Member;
import capstone.ballkeeper.domain.reservation.Reservation;
import capstone.ballkeeper.domain.reservation.ReservationStatus;
import capstone.ballkeeper.domain.reservation.dto.ReservationResponse;
import capstone.ballkeeper.event.ReservationCreatedEvent;
import capstone.ballkeeper.repository.ItemRepository;
import capstone.ballkeeper.repository.MemberRepository;
import capstone.ballkeeper.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ItemRepository itemRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /** 예약 생성 */
    @Transactional
    public Long createReservation(Long memberId, Long itemId,
                                  LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("시작/종료 시각은 필수입니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("시작 시각은 종료 시각보다 앞서야 합니다.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("물품이 존재하지 않습니다."));

        if (reservationRepository.existsOverlapping(itemId, startTime, endTime)) {
            throw new IllegalStateException("해당 시간대에 이미 예약이 존재합니다.");
        }

        Reservation reservation = Reservation.createReservation(
                member, startTime, endTime,
                ReservationStatus.RESERVED,  // 예약 생성 시점
                UsageStatus.PENDING          // 관리자 승인 대기
        );

        // 중간 엔티티 연결 + 상태 동기화
        reservation.addItem(item);
        item.changeStatus(ItemStatus.RESERVED);

        reservationRepository.save(reservation);

        applicationEventPublisher.publishEvent(new ReservationCreatedEvent(reservation.getId()));
        return reservation.getId();
    }

    /** 단건 조회 (엔티티) */
    public Reservation findReservation(Long reservationId) {
        return reservationRepository.findOne(reservationId);
    }

    /** 단건 조회 (DTO 변환용, fetch join) */
    @Transactional(readOnly = true)
    public ReservationResponse findReservationDto(Long reservationId) {
        Reservation r = reservationRepository.findOneWithItems(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다.");
        return ReservationResponse.from(r);
    }

    /** 목록 조회 (조건별, fetch join) → DTO 리스트 반환 */
    @Transactional(readOnly = true)
    public List<ReservationResponse> findReservations(Long memberId, String status) {
        List<Reservation> list;
        if (memberId != null && status != null) {
            list = reservationRepository.findByMemberIdAndStatusWithItems(memberId, parseStatus(status));
        } else if (memberId != null) {
            list = reservationRepository.findByMemberIdWithItems(memberId);
        } else if (status != null) {
            list = reservationRepository.findByStatusWithItems(parseStatus(status));
        } else {
            list = reservationRepository.findAllWithItems();
        }
        return list.stream().map(ReservationResponse::from).toList();
    }

    private ReservationStatus parseStatus(String status) {
        try {
            return ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 예약 상태입니다: " + status);
        }
    }

    /** 예약 취소 */
    @Transactional
    public void cancelReservation(Long reservationId) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다.");
        r.changeStatus(ReservationStatus.CANCELLED);
        // 아이템 상태 복구는 보통 픽업/반납 시점에 일관 관리
    }

    /** (관리자) 예약 승인 */
    @Transactional
    public void approveReservation(Long reservationId) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다.");
        if (r.getStatus() != ReservationStatus.RESERVED) {
            throw new IllegalStateException("RESERVED 상태만 승인할 수 있습니다.");
        }
        // 예약의 메인 상태는 유지, 관리자 승인만 반영
        r.updateUsageStatus(UsageStatus.APPROVED, LocalDateTime.now());
    }

    /** 픽업 처리 */
    @Transactional
    public void pickUp(Long reservationId, String pickupPhotoUrl) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다!");
        if (r.getUsageStatus() != UsageStatus.APPROVED) {
            throw new IllegalStateException("관리자 승인된 예약만 픽업할 수 있습니다.");
        }
        r.updatePickupInfo(LocalDateTime.now(), pickupPhotoUrl);
        r.changeStatus(ReservationStatus.IN_USE);
        r.getReservationItems().forEach(ri -> ri.getItem().changeStatus(ItemStatus.IN_USE));
    }

    /** 반납 처리 */
    @Transactional
    public void returnItem(Long reservationId, String returnPhotoUrl) {
        Reservation r = reservationRepository.findOne(reservationId);
        if (r == null) throw new IllegalArgumentException("예약이 존재하지 않습니다!");
        if (r.getStatus() != ReservationStatus.IN_USE) {
            throw new IllegalStateException("사용 중(IN_USE)인 예약만 반납할 수 있습니다.");
        }
        r.updateReturnInfo(LocalDateTime.now(), returnPhotoUrl);
        r.changeStatus(ReservationStatus.RETURNED);
        r.getReservationItems().forEach(ri -> ri.getItem().changeStatus(ItemStatus.AVAILABLE));
    }
}
