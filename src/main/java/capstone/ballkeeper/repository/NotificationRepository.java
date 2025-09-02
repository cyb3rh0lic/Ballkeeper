package capstone.ballkeeper.repository;

import capstone.ballkeeper.domain.notification.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 특정 사용자(memberId)의 전체 알림 (최신순)
    Page<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 특정 사용자(memberId)의 읽지 않은 알림만 (최신순)
    Page<Notification> findByMemberIdAndReadOrderByCreatedAtDesc(Long memberId, boolean read, Pageable pageable);

    // 특정 사용자(memberId)의 읽지 않은 알림 개수
    long countByMemberIdAndRead(Long memberId, boolean read);
}
