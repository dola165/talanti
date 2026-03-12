package ge.dola.talanti.club.dto;

public record ClubRosterDto(
        Long id,
        String name,
        String position,
        Integer number,
        String status,
        String avatar
) {}