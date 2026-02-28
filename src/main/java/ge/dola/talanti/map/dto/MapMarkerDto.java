package ge.dola.talanti.map.dto;

public record MapMarkerDto(
        Long entityId,
        String entityType,  // "CLUB", "USER"
        String title,       // Club name or User name
        String subtitle,    // "Amateur", "Forward"
        Double latitude,
        Double longitude,
        Double distanceKm   // How far away it is from the user's center point
) {}