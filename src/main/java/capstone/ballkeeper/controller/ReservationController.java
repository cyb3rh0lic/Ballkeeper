package capstone.ballkeeper.controller;

import capstone.ballkeeper.domain.reservation.Reservation;
import capstone.ballkeeper.domain.reservation.ReservationStatus;
import capstone.ballkeeper.domain.reservation.dto.ReservationCreateRequest;
import capstone.ballkeeper.domain.reservation.dto.ReservationResponse;
import capstone.ballkeeper.repository.ReservationRepository;
import capstone.ballkeeper.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;

    /** 예약 생성 */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest req) {
        Long id = reservationService.createReservation(
                req.memberId(), req.itemId(), req.startTime(), req.endTime());

        Reservation saved = reservationService.findReservation(id); // 서비스에 단건 조회 메서드 추가되어 있어야 함
        return ResponseEntity
                .created(URI.create("/api/reservations/" + id))
                .body(ReservationResponse.from(saved));
    }

    /** 예약 취소 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        reservationService.cancelReservation(id);
        return ResponseEntity.noContent().build();
    }

    /** 예약 단건 조회 */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> findOne(@PathVariable Long id) {
        Reservation r = reservationService.findReservation(id);
        if (r == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ReservationResponse.from(r));
    }

    /**
     * 예약 목록 조회
     * - memberId & status 동시 제공 시: 둘 다로 필터
     * - memberId만 제공: 회원별
     * - status만 제공: 상태별
     * - 둘 다 없으면 전체
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> findList(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status
    ) {
        List<Reservation> list;

        if (memberId != null && status != null) {
            ReservationStatus st = parseStatus(status);
            list = reservationRepository.findByMemberIdAndStatus(memberId, st);
        } else if (memberId != null) {
            list = reservationRepository.findByMemberId(memberId);
        } else if (status != null) {
            ReservationStatus st = parseStatus(status);
            list = reservationRepository.findByStatus(st);
        } else {
            list = reservationRepository.findAll();
        }

        return ResponseEntity.ok(list.stream().map(ReservationResponse::from).toList());
    }

    /*
    예약 승인(관리자)
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return ResponseEntity.noContent().build();
    }

    /** 픽업 처리 */
    @PatchMapping("/{id}/pickup")
    public ResponseEntity<Void> pickup(@PathVariable Long id, String pickupPhotoUrl) {
        reservationService.pickUp(id, pickupPhotoUrl);
        return ResponseEntity.ok().build();
    }

    /** 반납 처리 */
    @PatchMapping("/{id}/return")
    public ResponseEntity<Void> returnItem(@PathVariable Long id, String returnPhotoUrl) {
        reservationService.returnItem(id, returnPhotoUrl);
        return ResponseEntity.ok().build();
    }

    // 잘못된 status 문자열을 400(BAD_REQUEST)으로 처리하기 위해 파싱 유틸 분리
    private ReservationStatus parseStatus(String status) {
        try {
            return ReservationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("잘못된 예약 상태입니다: " + status);
        }
    }
}
