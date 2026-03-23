package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CreateChallengeDto(
        Long targetClubId,

        Long challengingSquadId,
        Long targetSquadId,

        @Pattern(
                regexp = "(?i)FRIENDLY|COMPETITIVE",
                message = "Match type must be FRIENDLY or COMPETITIVE"
        )
        String matchType,

        @NotNull(message = "A proposed date is required")
        @Future(message = "Proposed date must be in the future")
        LocalDateTime proposedDate,

        @Size(max = 500, message = "Message cannot exceed 500 characters")
        String message
) {}
