package ge.dola.talanti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CookieAuthFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "talanti_auth_token";

    private final TokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    public CookieAuthFilter(TokenProvider tokenProvider, CustomUserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractTokenFromCookie(request);

            if (token != null && tokenProvider.validateToken(token)) {
                // 1. Get user ID from the token
                Long userId = tokenProvider.getUserIdFromToken(token);

                // 2. Fetch fresh user details from the database using JOOQ
                CustomUserDetails userDetails = userDetailsService.loadUserById(userId);

                // 3. Tell Spring Security this user is authenticated for this request
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Log the error, but don't crash. Spring Security will just block the request
            // because the SecurityContext is empty.
            System.err.println("Could not set user authentication in security context: " + ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}