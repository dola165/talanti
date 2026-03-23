package ge.dola.talanti.user.dto;

import ge.dola.talanti.user.UserType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteProfileDto(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name cannot exceed 100 characters")
        String fullName,

        @NotNull(message = "Role is required")
        UserType role,

        @Size(max = 50, message = "Position cannot exceed 50 characters")
        String position,

        @Pattern(
                regexp = "(?i)RIGHT|LEFT|BOTH",
                message = "Preferred foot must be RIGHT, LEFT, or BOTH"
        )
        String preferredFoot,

        @Min(value = 100, message = "Height must be at least 100 cm")
        @Max(value = 250, message = "Height must be at most 250 cm")
        Integer heightCm,

        @Min(value = 30, message = "Weight must be at least 30 kg")
        @Max(value = 250, message = "Weight must be at most 250 kg")
        Integer weightKg,

        @Size(max = 1000, message = "Bio cannot exceed 1000 characters")
        String bio
) {}
