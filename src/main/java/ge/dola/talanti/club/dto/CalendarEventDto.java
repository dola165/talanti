package ge.dola.talanti.club.dto;

public record CalendarEventDto(
        String id,
        String type,
        String title,
        String date,
        String location,
        String status
) {}