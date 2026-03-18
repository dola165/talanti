package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record CalendarRequestDto(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Type is required") // 'MATCH', 'TRYOUT', 'MEETING'
        String type,

        @NotNull(message = "Date is required")
        LocalDateTime date,

        String location,
        Long targetLocationClubId,

        // Missing fields added to match your business logic
        String ageGroup,      // e.g., "U15", "U18", "SENIOR"
        String gender,        // "MALE", "FEMALE", "MIXED"
        Boolean willingToTravel
) {}