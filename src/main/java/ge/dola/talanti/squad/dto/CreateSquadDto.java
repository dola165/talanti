package ge.dola.talanti.squad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSquadDto(
        @NotBlank(message = "Squad name is required (e.g., 'First Team')")
        @Size(max = 100, message = "Squad name cannot exceed 100 characters")
        String name,

        @NotBlank(message = "Category is required (e.g., 'U15', 'SENIOR')")
        @Size(max = 32, message = "Category cannot exceed 32 characters")
        String category,

        @NotBlank(message = "Gender category is required (e.g., 'MALE', 'FEMALE', 'MIXED')")
        @Pattern(regexp = "MALE|FEMALE|MIXED", message = "Gender must be MALE, FEMALE, or MIXED")
        String gender
) {}
