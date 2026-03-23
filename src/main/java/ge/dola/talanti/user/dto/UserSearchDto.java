package ge.dola.talanti.user.dto;

public record UserSearchDto(
        Long id,
        String fullName,
        String username,
        String position,
        String userType,
        String avatarUrl
) {}
