package ge.dola.talanti.club.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateHonourDto(
        @NotBlank(message = "Trophy/Honour title is required")
        @Size(max = 120, message = "Title cannot exceed 120 characters")
        String title,

        @NotNull(message = "Year won is required")
        @Min(value = 1850, message = "Year won must be realistic")
        @Max(value = 2100, message = "Year won must be realistic")
        Integer yearWon,

        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        String description
) {}
