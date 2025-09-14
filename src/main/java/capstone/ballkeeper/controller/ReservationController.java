package capstone.ballkeeper.controller;

import capstone.ballkeeper.domain.reservation.dto.ReservationCreateRequest;
import capstone.ballkeeper.domain.reservation.dto.ReservationResponse;
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

    /** 예약 생성 */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody ReservationCreateRequest req) {
        Long id = reservationService.createReservation(
                req.memberId(), req.itemId(), req.startTime(), req.endTime());

        // DTO 전용 단건 조회(연관 로딩 포함)로 응답
        ReservationResponse body = reservationService.findReservationDto(id);
        return ResponseEntity
                .created(URI.create("/api/reservations/" + id))
                .body(body);
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
        return ResponseEntity.ok(reservationService.findReservationDto(id));
    }

    /**
     * 예약 목록 조회
     * - memberId & status: 둘 다로 필터
     * - memberId만: 회원별
     * - status만: 상태별
     * - 둘 다 없음: 전체
     */
    @GetMapping
    public ResponseEntity<List<ReservationResponse>> findList(
            @RequestParam(required = false) Long memberId,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(reservationService.findReservations(memberId, status));
    }

    /** (관리자) 예약 승인 */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        reservationService.approveReservation(id);
        return ResponseEntity.noContent().build();
    }

    /** 픽업 처리 */
    @PatchMapping("/{id}/pickup")
    public ResponseEntity<Void> pickup(@PathVariable Long id,
                                       @RequestParam(required = false) String pickupPhotoUrl) {
        reservationService.pickUp(id, pickupPhotoUrl);
        return ResponseEntity.noContent().build();
    }

    /** 반납 처리 */
    @PatchMapping("/{id}/return")
    public ResponseEntity<Void> returnItem(@PathVariable Long id,
                                           @RequestParam(required = false) String returnPhotoUrl) {
        reservationService.returnItem(id, returnPhotoUrl);
        return ResponseEntity.noContent().build();
    }
}
