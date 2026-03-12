package ge.dola.talanti.tryout.dto;

import java.util.Map;

public record TryoutApplicantDto(
        Long id,
        Long userId,
        String name,
        String position,
        String ageGroup,
        String status,
        Integer matchScore,
        Map<String, Integer> attributes
) {}