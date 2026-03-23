package ge.dola.talanti.squad.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AddSquadPlayerDto(
        @NotNull(message = "User ID is required")
        @Positive(message = "User ID must be positive")
        Long userId,

        @Positive(message = "Jersey number must be positive")
        Integer jerseyNumber,

        @Size(max = 32, message = "Squad role cannot exceed 32 characters")
        @Pattern(regexp = "PLAYER|CAPTAIN|TRIALIST", message = "Squad role must be PLAYER, CAPTAIN, or TRIALIST")
        String squadRole // e.g., 'PLAYER', 'CAPTAIN', 'TRIALIST'
) {}
