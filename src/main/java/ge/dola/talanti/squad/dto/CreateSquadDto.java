package ge.dola.talanti.squad.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSquadDto(
        @NotBlank(message = "Squad name is required (e.g., 'First Team')")
        String name,

        @NotBlank(message = "Category is required (e.g., 'U15', 'SENIOR')")
        String category,

        @NotBlank(message = "Gender category is required (e.g., 'MALE', 'FEMALE', 'MIXED')")
        String gender
) {}