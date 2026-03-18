package ge.dola.talanti.user.event;

public record UserRoleChangedEvent(Long userId, String oldRole, String newRole) {}