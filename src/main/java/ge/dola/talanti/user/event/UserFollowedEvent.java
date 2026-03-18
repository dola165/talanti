package ge.dola.talanti.user.event;

public record UserFollowedEvent(Long followerId, Long followingId) {}