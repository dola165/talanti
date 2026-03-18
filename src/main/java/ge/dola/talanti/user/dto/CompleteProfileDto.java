package ge.dola.talanti.user.dto;

import ge.dola.talanti.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CompleteProfileDto(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotNull(message = "Role is required")
        UserType role,

        String position,
        String preferredFoot,
        Integer heightCm,
        Integer weightKg,
        String bio
) {}