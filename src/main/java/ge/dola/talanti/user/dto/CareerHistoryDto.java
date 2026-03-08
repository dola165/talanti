package ge.dola.talanti.user.dto;

public record CareerHistoryDto(
        Long id,
        String clubName,
        String season,
        String category,
        int appearances,
        int goals,
        int assists,
        int cleanSheets
) {}