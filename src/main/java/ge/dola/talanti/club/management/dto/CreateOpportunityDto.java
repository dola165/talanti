package ge.dola.talanti.club.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOpportunityDto(
        @NotBlank(message = "Opportunity type is required")
        @Pattern(regexp = "(?i)^(FUNDRAISING|JOB|VOLUNTEER|WISHLIST)$", message = "Invalid opportunity type")
        String type,

        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title cannot exceed 120 characters")
        String title,

        @Size(max = 2000, message = "Description cannot exceed 2000 characters")
        String description,

        @Pattern(
                regexp = "^(?:https?://.+)?$",
                message = "External link must be a valid http or https URL"
        )
        String externalLink
) {}
