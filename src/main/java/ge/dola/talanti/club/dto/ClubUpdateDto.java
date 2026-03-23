package ge.dola.talanti.club.dto;

import jakarta.validation.constraints.Pattern;

public record ClubUpdateDto(
        @Pattern(
                regexp = "^(?:/uploads/.+|https?://.+)$",
                message = "Logo URL must be an uploaded path or an http/https URL"
        )
        String logoUrl,

        @Pattern(
                regexp = "^(?:/uploads/.+|https?://.+)$",
                message = "Banner URL must be an uploaded path or an http/https URL"
        )
        String bannerUrl
) {}
