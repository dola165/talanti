package ge.dola.talanti.user;

import ge.dola.talanti.user.dto.CompleteProfileDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.enums.SystemRole;
import ge.dola.talanti.user.repository.UserProfileRepository;
import ge.dola.talanti.user.repository.UserRepository;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DSLContext dsl;

    public UserService(UserRepository userRepository, UserProfileRepository userProfileRepository, DSLContext dsl) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.dsl = dsl;
    }

    @Transactional
    public void completeUserProfile(Long userId, CompleteProfileDto dto) {

        // 1. Safely parse the Role String ("PLAYER", "CLUB_ADMIN", etc.) into your enum's integer code
        short systemRoleCode;
        try {
            systemRoleCode = (short) SystemRole.valueOf(dto.role()).getCode();
        } catch (IllegalArgumentException e) {
            systemRoleCode = (short) SystemRole.FAN.getCode(); // Fallback to 0
        }

        // 2. Update the USERS table (Role)
        dsl.update(USERS)
                .set(USERS.SYSTEM_ROLE, systemRoleCode)
                .where(USERS.ID.eq(userId))
                .execute();

        // 3. Update the USER_PROFILES table (Stats, Name, Bio)
        dsl.update(USER_PROFILES)
                .set(USER_PROFILES.FULL_NAME, dto.fullName())
                .set(USER_PROFILES.POSITION, dto.position())
                .set(USER_PROFILES.PREFERRED_FOOT, dto.preferredFoot())
                .set(USER_PROFILES.HEIGHT_CM, dto.heightCm())
                .set(USER_PROFILES.WEIGHT_KG, dto.weightKg())
                .set(USER_PROFILES.BIO, dto.bio())
                .where(USER_PROFILES.USER_ID.eq(userId))
                .execute();
    }
    public PublicUserProfileDto getProfile(Long targetUserId, Long currentUserId) {
        return userProfileRepository.getPublicProfile(targetUserId, currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public boolean toggleFollow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            throw new RuntimeException("You cannot follow yourself");
        }

        boolean isFollowing = userRepository.isFollowingUser(currentUserId, targetUserId);

        if (isFollowing) {
            userRepository.unfollowUser(currentUserId, targetUserId);
            return false;
        } else {
            userRepository.followUser(currentUserId, targetUserId);
            return true;
        }
    }
}