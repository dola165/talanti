package ge.dola.talanti.dto;

import java.time.LocalDateTime;

public class UserDTOs {
    public record UserSummary(
            Long id,
            String username,
            String fullName, // From user_profiles
            String avatarUrl // Optional
    ) {}
    }