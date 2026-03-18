package ge.dola.talanti.club.dto;

public record ClubOpportunityDto(
        Long id,
        String type, // 'FUNDRAISING', 'JOB', 'VOLUNTEER', 'WISHLIST'
        String title,
        String externalLink // e.g., the GoFundMe or Facebook Store URL
) {}