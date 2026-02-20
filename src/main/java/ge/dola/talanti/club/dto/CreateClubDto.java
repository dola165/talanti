package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 2. Create DTO (Strict validation for incoming data)
public record CreateClubDto(
        @NotBlank(message = "Club name cannot be empty")
        @Size(max = 255, message = "Name is too long")
        String name,

        String description,

        Long locationId,

        @NotBlank(message = "Club type is required")
        String type,

        @NotNull(message = "Official status must be specified")
        Boolean isOfficial
) {}