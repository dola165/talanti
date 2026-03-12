package ge.dola.talanti.club.dto;

public record CalendarRequestDto(
        String title,
        String type,
        String date,
        String location,
        Long targetLocationClubId // NEW: Tells the backend which map pin to use
) {}