package ge.dola.talanti.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ge.dola.talanti.security.rate.RedisRateLimitingFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Enforces our strict SpEL checks!
public class SecurityConfig {

    private final JwtService jwtService;
    private final ObjectProvider<RedisRateLimitingFilter> rateLimitingFilterProvider;

    public SecurityConfig(JwtService jwtService, ObjectProvider<RedisRateLimitingFilter> rateLimitingFilterProvider) {
        this.jwtService = jwtService;
        this.rateLimitingFilterProvider = rateLimitingFilterProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(jwtService);

        RedisRateLimitingFilter rateLimitingFilter = rateLimitingFilterProvider.getIfAvailable();

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(cookieCsrfTokenRepository())
                        .requireCsrfProtectionMatcher(request -> {
                            String path = request.getRequestURI();
                            if (path == null) {
                                return false;
                            }
                            return "POST".equalsIgnoreCase(request.getMethod())
                                    && (path.equals("/api/auth/refresh") || path.equals("/api/auth/logout"));
                        })
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/swagger-resources/**", "/webjars/**", "/favicon.ico"
                        ).permitAll()

                        // Authentication Endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // STRICT ENFORCEMENT: Open public reads so the frontend can actually render for guests
                        .requestMatchers(HttpMethod.GET,
                                "/api/posts/**",
                                "/api/clubs",
                                "/api/clubs/*",
                                "/api/clubs/*/roster",
                                "/api/clubs/*/calendar",
                                "/api/clubs/*/squads",
                                "/api/clubs/*/squads/*/roster",
                                "/api/map/nearby", // Allows the map to render pins
                                "/api/media/**",   // Allows viewing uploaded images
                                "/api/tryouts/**",  // Allows viewing tryout details
                                "/uploads/**"       // Uploaded files are rendered directly by <img> tags
                        ).permitAll()

                        // Everything else (Writes, User settings, Admin panels) requires a token
                        .anyRequest().authenticated()
                );

        if (rateLimitingFilter != null) {
            http.addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class);
        }

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
