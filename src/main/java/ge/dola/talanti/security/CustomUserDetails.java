package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
public class CustomUserDetails implements UserDetails {

    private final UsersRecord user;

    public CustomUserDetails(UsersRecord user) {
        this.user = user;
    }

    public Long getId() {
        return user.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Mapping your system_role (Short) to a Spring Security Role.
        // E.g., 0 = User, 1 = Admin. Adjust this logic as you define roles.
        String roleName = user.getSystemRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";
        return Collections.singletonList(new SimpleGrantedAuthority(roleName));
    }

    @Override
    public String getPassword() {
        // For OAuth2 users, this is usually null or empty.
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail(); // We use email as the primary identifier
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}