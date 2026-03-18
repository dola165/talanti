package ge.dola.talanti.club.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOpportunityDto(
        @NotBlank(message = "Opportunity type is required")
        @Pattern(regexp = "^(FUNDRAISING|JOB|VOLUNTEER|WISHLIST)$", message = "Invalid opportunity type")
        String type,

        @NotBlank(message = "Title is required")
        String title,

        String description,
        String externalLink
) {}