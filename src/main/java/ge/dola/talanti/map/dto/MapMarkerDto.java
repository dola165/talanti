package ge.dola.talanti.map.dto;

public record MapMarkerDto(
        Long entityId,
        String entityType,  // "CLUB", "TRYOUT", "MATCH_REQUEST"
        String title,
        String subtitle,
        Double latitude,
        Double longitude,
        Double distanceKm,

        // --- NEW FIELDS FOR FIGMA UI ---
        Integer members,
        Integer followers,
        Boolean verified,
        String date,        // For matches and tryouts
        String fee          // e.g. "Free", "$50"
) {}