package capstone.ballkeeper.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class NotificationPayload {
    String type;            // "RESERVATION_CREATED"
    Long reservationId;
    Long userId;
    Long itemId;
    String photoUrl;
    String message;
    LocalDateTime createdAt;
}
