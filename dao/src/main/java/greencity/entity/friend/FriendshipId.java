package greencity.entity.friend;

import jakarta.persistence.Embeddable;

@Embeddable
public record FriendshipId(Long userId, Long friendId){}