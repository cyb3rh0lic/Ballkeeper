package capstone.ballkeeper.domain.notification;

import capstone.ballkeeper.domain.item.Item;
import capstone.ballkeeper.domain.member.Member;
import capstone.ballkeeper.domain.reservation.Reservation;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATIONS")
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id @GeneratedValue
    @Column(name = "NOTIFICATION_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RESERVATION_ID", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ITEM_ID")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", length = 40, nullable = false)
    private NotificationType type;

    @Column(length = 100, nullable = false)
    private String message;

    @Column(name = "PHOTO_URL", length = 100)
    private String photoUrl;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "IS_READ", nullable = false)
    private boolean read;   // 기본 false

    // 생성 메서드
    public static Notification createNotification(Reservation reservation, Member member, Item item, NotificationType type, String message, String photoUrl) {
        Notification notification = new Notification();
        notification.reservation = reservation;
        notification.member = member;
        notification.item = item;
        notification.type = type;
        notification.message = message;
        notification.photoUrl = photoUrl;
        notification.createdAt = LocalDateTime.now();
        notification.read = false;

        return notification;
    }

    public void markRead() {
        this.read = true;
    }
}
