package ge.dola.talanti.user.dto;

import ge.dola.talanti.user.UserType;

public record UserProfileDto(Long id,
                             String username,
                             String email,
                             String fullName,
                             String position,
                             String bio,
                             UserType role
) {}