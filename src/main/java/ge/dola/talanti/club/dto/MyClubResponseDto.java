package ge.dola.talanti.club.dto;

public record MyClubResponseDto(
        Long clubId,
        String clubName,
        String myRole // e.g., "OWNER", "COACH", "PLAYER"
) {}