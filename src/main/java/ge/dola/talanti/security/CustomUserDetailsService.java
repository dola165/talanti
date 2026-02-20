package ge.dola.talanti.security;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import ge.dola.talanti.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Standard Spring Security lookup
    @Override
    public CustomUserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UsersRecord user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);
    }

    // Custom lookup used by our CookieAuthFilter later
    public CustomUserDetails loadUserById(Long id) {
        UsersRecord user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return new CustomUserDetails(user);
    }
}