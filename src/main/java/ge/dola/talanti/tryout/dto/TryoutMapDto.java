package ge.dola.talanti.tryout.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TryoutMapDto(
        Long id,
        Long clubId,
        String clubName,
        String title,
        String description,
        String position,
        String ageGroup,
        LocalDateTime tryoutDate,
        BigDecimal latitude,
        BigDecimal longitude,
        String addressText
) {}