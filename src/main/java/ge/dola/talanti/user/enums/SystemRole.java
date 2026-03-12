package ge.dola.talanti.user.enums;

public enum SystemRole {
    FAN(0),
    PLAYER(1),
    AGENT(2),
    CLUB_ADMIN(3);

    private final int code;

    SystemRole(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static SystemRole fromCode(Number code) {
        if (code == null) return FAN;
        for (SystemRole role : values()) {
            if (role.code == code.intValue()) return role;
        }
        return FAN; // Default fallback
    }
}