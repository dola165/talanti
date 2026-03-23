package ge.dola.talanti.club.dto;

import ge.dola.talanti.club.ClubCommunicationMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateClubDto(
        @NotBlank(message = "Club name cannot be empty")
        @Size(max = 120, message = "Name is too long")
        String name,

        @Size(max = 2000, message = "Description is too long")
        String description,

        Long locationId,

        @NotBlank(message = "Club type is required")
        @Pattern(regexp = "(?i)PROFESSIONAL|GRASSROOTS|ACADEMY", message = "Club type must be PROFESSIONAL, GRASSROOTS, or ACADEMY")
        String type,

        @Email(message = "Contact email must be valid")
        @Size(max = 255, message = "Contact email is too long")
        String contactEmail,

        @Size(max = 50, message = "WhatsApp number is too long")
        String whatsappNumber,

        @Size(max = 255, message = "Facebook/Messenger URL is too long")
        String facebookMessengerUrl,

        @NotNull(message = "Preferred communication method is required")
        ClubCommunicationMethod preferredCommunicationMethod,

        Boolean isOfficial
) {}
