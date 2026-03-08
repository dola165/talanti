package ge.dola.talanti.map.dto;

import java.time.LocalDateTime;

public record MatchRequestDto(
        Long id,
        Long clubId,
        Long squadId,
        String locationPref, // "CAN_HOST", "WILL_TRAVEL", "NEUTRAL"
        LocalDateTime desiredDate
) {}