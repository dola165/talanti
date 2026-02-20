package ge.dola.talanti.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CookieAuthFilter cookieAuthFilter;

    // Injecting the filter we will create next
    public SecurityConfig(CookieAuthFilter cookieAuthFilter) {
        this.cookieAuthFilter = cookieAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF for now to simplify React integration.
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Enable CORS so React can safely send requests to our API.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 3. Make session strictly stateless. No JSESSIONID will be generated.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. Secure the routes
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Allow everyone to attempt login
                        .requestMatchers("/api/public/**").permitAll() // For future public club/player searches
                        .anyRequest().authenticated() // Everything else requires a valid cookie
                )

                // 5. Add our custom Cookie filter BEFORE Spring checks for standard passwords
                .addFilterBefore(cookieAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Ensure this matches your React frontend URL exactly
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));

        // CRITICAL: This explicitly allows the browser to send the HttpOnly cookie
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}