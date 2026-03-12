package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
import static ge.dola.talanti.util.LogSafe.safe;
import static ge.dola.talanti.util.LogSafe.safeEmail;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final DSLContext dsl;

    public CustomUserDetailsService(DSLContext dsl) {
        this.dsl = dsl;
    }

    // Used during initial Login (Auth Controller)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UsersRecord userRecord = dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + safeEmail(email)));

        return buildCustomUserDetails(userRecord);
    }

    // Used by our JWT Filter to validate requests using the User ID stored in the token
    public CustomUserDetails loadUserById(Long id) throws UsernameNotFoundException {
        UsersRecord userRecord = dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + safe(id)));

        return buildCustomUserDetails(userRecord);
    }

    // Centralized helper to build the UserDetails object safely
    private CustomUserDetails buildCustomUserDetails(UsersRecord userRecord) {
        // Map smallint (0 or 1) to Spring Security Roles
        String roleStr = userRecord.getSystemRole() == 1 ? "ROLE_ADMIN" : "ROLE_USER";

        // Try to fetch their real full name from the profiles table
        String fullName = dsl.select(USER_PROFILES.FULL_NAME)
                .from(USER_PROFILES)
                .where(USER_PROFILES.USER_ID.eq(userRecord.getId()))
                .fetchOptionalInto(String.class)
                .orElse(userRecord.getUsername()); // Fallback to username if no profile yet

        // Spring Security throws an error if the 'username' field is null.
        // If email is null (e.g., registered via phone), we fallback to the username.
        String loginIdentifier = userRecord.getEmail() != null ? userRecord.getEmail() : userRecord.getUsername();

        return new CustomUserDetails(
                userRecord.getId(),
                loginIdentifier,
                userRecord.getPasswordHash(),
                roleStr.replace("ROLE_", ""), // Pass cleanly as "ADMIN" or "USER"
                fullName,
                List.of(new SimpleGrantedAuthority(roleStr))
        );
    }
}