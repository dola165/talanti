package ge.dola.talanti.squad.dto;

public record SquadDto(
        Long id,
        Long clubId,
        String name,
        String category,
        String gender,
        Long headCoachId
) {}