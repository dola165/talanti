package ge.dola.talanti.map.dto;

public record MapMarkerDto(
        Long entityId,
        String entityType,  // "CLUB", "TRYOUT", "MATCH"
        String title,
        String subtitle,
        String clubName,
        Double latitude,
        Double longitude,
        Double distanceKm,

        // --- NEW FIELDS FOR FIGMA UI ---
        Integer members,
        Integer followers,
        Boolean verified,
        String date,        // For matches and tryouts
        String fee,         // e.g. "Free", "$50"
        String addressText,
        String ageGroup,
        String status
) {}
