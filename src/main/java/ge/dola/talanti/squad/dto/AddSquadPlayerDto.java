package ge.dola.talanti.squad.dto;

import jakarta.validation.constraints.NotNull;

public record AddSquadPlayerDto(
        @NotNull(message = "User ID is required")
        Long userId,

        Integer jerseyNumber,

        String squadRole // e.g., 'PLAYER', 'CAPTAIN', 'TRIALIST'
) {}