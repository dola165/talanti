package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CreateChallengeDto(
        @NotNull
        Long targetClubId,

        Long challengingSquadId, // Null if they are just doing a generic U15 challenge
        Long targetSquadId,      // Null if challenging the whole club generically

        String matchType,        // 'FRIENDLY', 'COMPETITIVE'
        LocalDateTime proposedDate,
        String message           // "Hey, our U15 boys want to play your U15 boys"
) {}