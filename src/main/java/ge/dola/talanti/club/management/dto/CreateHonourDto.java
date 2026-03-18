package ge.dola.talanti.club.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateHonourDto(
        @NotBlank(message = "Trophy/Honour title is required")
        String title,

        @NotNull(message = "Year won is required")
        Integer yearWon,

        String description
) {}