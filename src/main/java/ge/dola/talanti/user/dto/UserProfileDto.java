package ge.dola.talanti.user.dto;

public record UserProfileDto(Long id,
                             String username,
                             String email,
                             String fullName,
                             String position,
                             String bio,
                             String role
) {}