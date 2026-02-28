package ge.dola.talanti.map.dto;

public record SaveLocationDto(
        String entityType,
        Long entityId,
        Double latitude,
        Double longitude,
        String addressText
) {}