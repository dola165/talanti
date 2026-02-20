package ge.dola.talanti.club.dto;

import java.time.LocalDateTime;

// 1. Basic Read DTO (No validation, just data transfer)
public record ClubDto(
        Long id,
        String name,
        String description,
        Long locationId,
        String type,
        boolean isOfficial,
        LocalDateTime createdAt
) {}