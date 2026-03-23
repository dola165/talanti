package ge.dola.talanti.club;

import java.util.List;
import java.util.Locale;

public enum ClubRole {
    OWNER(0),
    CLUB_ADMIN(1),
    COACH(2),
    AGENT(3),
    PLAYER(4);

    private final int sortOrder;

    ClubRole(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int sortOrder() {
        return sortOrder;
    }

    public boolean isLeadershipRole() {
        return this == OWNER || this == CLUB_ADMIN;
    }

    public boolean isStaffRole() {
        return this != PLAYER;
    }

    public boolean canReviewTryouts() {
        return isLeadershipRole() || this == COACH;
    }

    public List<ClubRole> assignableRoles() {
        return switch (this) {
            case OWNER -> List.of(CLUB_ADMIN, COACH, AGENT, PLAYER);
            case CLUB_ADMIN -> List.of(COACH, AGENT, PLAYER);
            default -> List.of();
        };
    }

    public boolean canAssignRole(ClubRole targetRole) {
        return assignableRoles().contains(targetRole);
    }

    public static ClubRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Club role is required.");
        }

        try {
            return ClubRole.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported club role.");
        }
    }
}
