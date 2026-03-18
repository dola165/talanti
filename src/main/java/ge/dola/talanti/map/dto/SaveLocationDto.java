package ge.dola.talanti.map.dto;

public record SaveLocationDto(
        Double latitude,
        Double longitude,
        String addressText
) {}