package ge.dola.talanti.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteProfileDto(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(PLAYER|CLUB_ADMIN|FAN)$", message = "Invalid role")
        String role,

        String position,
        String preferredFoot,
        Integer heightCm,
        Integer weightKg,
        String bio
) {}