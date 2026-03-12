package ge.dola.talanti.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final Long id;
    private final String role;       // e.g. "ADMIN", "USER"
    private final String fullName;

    public CustomUserDetails(Long id, String emailOrUsername, String password,
                             String role, String fullName,
                             Collection<? extends GrantedAuthority> authorities) {
        super(emailOrUsername, password, authorities);
        this.id = id;
        this.role = role;
        this.fullName = fullName;
    }

    public Long getUserId() { return id; }

    /** * Safe utility for business logic checks (not for Spring annotations).
     * Usage: if (user.hasRole("ADMIN")) {...}
     */
    public boolean hasRole(String roleName) {
        String normalized = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(normalized::equalsIgnoreCase);
    }
}