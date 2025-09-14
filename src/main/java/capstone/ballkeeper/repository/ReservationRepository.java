package capstone.ballkeeper.repository;

import capstone.ballkeeper.domain.reservation.Reservation;
import capstone.ballkeeper.domain.reservation.ReservationStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReservationRepository {

    private final EntityManager em;

    // 저장 (신규: persist / 수정: merge)
    public void save(Reservation reservation) {
        if (reservation.getId() == null) {
            em.persist(reservation);
        } else {
            em.merge(reservation);
        }
    }

    // 기본 단건 조회 (연관 로딩 없음)
    public Reservation findOne(Long id) {
        return em.find(Reservation.class, id);
    }

    // ===== fetch join 버전들 =====

    // 단건 + 연관(회원, 예약아이템, 아이템) 로딩
    public Reservation findOneWithItems(Long id) {
        List<Reservation> list = em.createQuery(
                        "select distinct r from Reservation r " +
                                "left join fetch r.member m " +
                                "left join fetch r.reservationItems ri " +
                                "left join fetch ri.item i " +
                                "where r.id = :id", Reservation.class)
                .setParameter("id", id)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    // 전체 + 연관 로딩
    public List<Reservation> findAllWithItems() {
        return em.createQuery(
                        "select distinct r from Reservation r " +
                                "left join fetch r.member m " +
                                "left join fetch r.reservationItems ri " +
                                "left join fetch ri.item i", Reservation.class)
                .getResultList();
    }

    // 회원별 + 연관 로딩
    public List<Reservation> findByMemberIdWithItems(Long memberId) {
        return em.createQuery(
                        "select distinct r from Reservation r " +
                                "left join fetch r.member m " +
                                "left join fetch r.reservationItems ri " +
                                "left join fetch ri.item i " +
                                "where m.id = :memberId", Reservation.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    // 상태별 + 연관 로딩
    public List<Reservation> findByStatusWithItems(ReservationStatus status) {
        return em.createQuery(
                        "select distinct r from Reservation r " +
                                "left join fetch r.member m " +
                                "left join fetch r.reservationItems ri " +
                                "left join fetch ri.item i " +
                                "where r.status = :status", Reservation.class)
                .setParameter("status", status)
                .getResultList();
    }

    // 회원+상태 복합 + 연관 로딩
    public List<Reservation> findByMemberIdAndStatusWithItems(Long memberId, ReservationStatus status) {
        return em.createQuery(
                        "select distinct r from Reservation r " +
                                "left join fetch r.member m " +
                                "left join fetch r.reservationItems ri " +
                                "left join fetch ri.item i " +
                                "where m.id = :memberId and r.status = :status", Reservation.class)
                .setParameter("memberId", memberId)
                .setParameter("status", status)
                .getResultList();
    }

    // 시간대 중복 예약 존재 여부
    public boolean existsOverlapping(Long itemId, LocalDateTime start, LocalDateTime end) {
        String jpql =
                "select count(r) " +
                        "from Reservation r " +
                        "join r.reservationItems ri " +
                        "where ri.item.id = :itemId " +
                        "and r.status in (:active) " +
                        "and r.endTime  > :start " +
                        "and r.startTime < :end";

        Long cnt = em.createQuery(jpql, Long.class)
                .setParameter("itemId", itemId)
                .setParameter("start", start)
                .setParameter("end", end)
                .setParameter("active", List.of(ReservationStatus.RESERVED, ReservationStatus.IN_USE))
                .getSingleResult();

        return cnt != null && cnt > 0L;
    }
}

