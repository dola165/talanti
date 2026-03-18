package ge.dola.talanti.security.authz;

import ge.dola.talanti.security.CustomUserDetails;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;

import java.util.function.Supplier;

public interface DomainAccessManager<T> extends AuthorizationManager<T> {

    @Override
    default AuthorizationResult authorize(Supplier<? extends Authentication> authenticationSupplier, T targetId) {
        Authentication auth = authenticationSupplier.get();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
            return new AuthorizationDecision(false);
        }

        // Universal bypass for System Admins
        if (user.hasRole("SYSTEM_ADMIN")) {
            return new AuthorizationDecision(true);
        }

        return new AuthorizationDecision(decide(user, targetId));
    }

    // Concrete classes implement this boolean check
    boolean decide(CustomUserDetails user, T targetId);
}