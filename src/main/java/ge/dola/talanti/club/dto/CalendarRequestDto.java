package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CalendarRequestDto(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title cannot exceed 120 characters")
        String title,

        @NotBlank(message = "Type is required")
        @Pattern(
                regexp = "TRYOUT|AVAILABILITY",
                message = "Type must be TRYOUT or AVAILABILITY"
        )
        String type,

        @NotNull(message = "Date is required")
        LocalDateTime date,

        @Size(max = 255, message = "Location cannot exceed 255 characters")
        String location,
        Long targetLocationClubId,

        @Size(max = 32, message = "Age group cannot exceed 32 characters")
        String ageGroup,

        @Pattern(
                regexp = "MALE|FEMALE|MIXED",
                message = "Gender must be MALE, FEMALE, or MIXED"
        )
        String gender,

        Boolean willingToTravel
) {}
