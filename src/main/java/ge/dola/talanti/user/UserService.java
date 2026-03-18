package ge.dola.talanti.user;

import ge.dola.talanti.user.dto.CompleteProfileDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.user.event.UserFollowedEvent;
import ge.dola.talanti.user.event.UserRoleChangedEvent;
import ge.dola.talanti.user.repository.UserProfileRepository;
import ge.dola.talanti.user.repository.UserRepository;
import ge.dola.talanti.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static ge.dola.talanti.jooq.Tables.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void completeUserProfile(Long userId, CompleteProfileDto dto) {
        log.info("Initiating profile completion for User ID: {}", userId);

        // Fetch current role to determine state delta
        String oldRoleStr = dsl.select(USERS.USER_TYPE)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOneInto(String.class);

        UserType oldRole = oldRoleStr != null ? UserType.valueOf(oldRoleStr) : null;
        UserType newRole = dto.role();

        // 1. Update Identity
        dsl.update(USERS)
                .set(USERS.USER_TYPE, newRole.name()) // Map Enum to jOOQ String
                .where(USERS.ID.eq(userId))
                .execute();

        // 2. Upsert Visual Profile
        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, userId)
                .set(USER_PROFILES.FULL_NAME, dto.fullName())
                .set(USER_PROFILES.BIO, dto.bio())
                .onDuplicateKeyUpdate()
                .set(USER_PROFILES.FULL_NAME, dto.fullName())
                .set(USER_PROFILES.BIO, dto.bio())
                .execute();

        // 3. Domain Extensions & Orphan Cleanup
        if (oldRole != null && oldRole != newRole) {
            cleanUpOrphanedDomainData(userId, oldRole);
        }

        if (newRole == UserType.PLAYER) {
            dsl.insertInto(PLAYER_DETAILS)
                    .set(PLAYER_DETAILS.USER_ID, userId)
                    .set(PLAYER_DETAILS.PRIMARY_POSITION, dto.position())
                    .set(PLAYER_DETAILS.PREFERRED_FOOT, dto.preferredFoot())
                    .set(PLAYER_DETAILS.HEIGHT_CM, dto.heightCm())
                    .set(PLAYER_DETAILS.WEIGHT_KG, dto.weightKg())
                    .onDuplicateKeyUpdate()
                    .set(PLAYER_DETAILS.PRIMARY_POSITION, dto.position())
                    .set(PLAYER_DETAILS.PREFERRED_FOOT, dto.preferredFoot())
                    .set(PLAYER_DETAILS.HEIGHT_CM, dto.heightCm())
                    .set(PLAYER_DETAILS.WEIGHT_KG, dto.weightKg())
                    .execute();
        } else if (newRole == UserType.AGENT) {
            // Inserts the base agent record. Fields like agency_name can be updated later if not in this DTO.
            dsl.insertInto(AGENT_DETAILS)
                    .set(AGENT_DETAILS.USER_ID, userId)
                    .onDuplicateKeyIgnore()
                    .execute();
        }

        // 4. Decoupled Notification
        if (oldRole != null && oldRole != newRole) {
            log.info("User ID: {} changed role from {} to {}. Publishing event.", userId, oldRole, newRole);
            eventPublisher.publishEvent(new UserRoleChangedEvent(userId, oldRole.name(), newRole.name()));
        }
    }

    private void cleanUpOrphanedDomainData(Long userId, UserType oldRole) {
        log.info("Cleaning up orphaned domain data for User ID: {}, Old Role: {}", userId, oldRole);
        switch (oldRole) {
            case PLAYER -> dsl.deleteFrom(PLAYER_DETAILS).where(PLAYER_DETAILS.USER_ID.eq(userId)).execute();
            case AGENT -> dsl.deleteFrom(AGENT_DETAILS).where(AGENT_DETAILS.USER_ID.eq(userId)).execute();
            // FAN and SYSTEM_ADMIN have no dedicated domain extension tables
            default -> {}
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PublicUserProfileDto getProfile(Long targetUserId, Long currentUserId) {
        log.debug("User ID: {} fetching public profile for Target ID: {}", currentUserId, targetUserId);
        return userProfileRepository.getPublicProfile(targetUserId, currentUserId)
                .orElseThrow(() -> new RuntimeException("User profile not found."));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public boolean toggleFollow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            log.warn("User ID: {} attempted to follow themselves. Blocked.", currentUserId);
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        boolean isFollowing = userRepository.isFollowingUser(currentUserId, targetUserId);

        if (isFollowing) {
            log.info("User ID: {} unfollowing Target ID: {}", currentUserId, targetUserId);
            userRepository.unfollowUser(currentUserId, targetUserId);
            return false;
        } else {
            log.info("User ID: {} following Target ID: {}", currentUserId, targetUserId);
            userRepository.followUser(currentUserId, targetUserId);

            // DECOUPLED NOTIFICATION: Broadcast the follow event
            eventPublisher.publishEvent(new UserFollowedEvent(currentUserId, targetUserId));
            return true;
        }
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getMyProfileData(Long userId) {
        var record = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        var profileName = dsl.select(USER_PROFILES.FULL_NAME)
                .from(USER_PROFILES)
                .where(USER_PROFILES.USER_ID.eq(userId))
                .fetchOneInto(String.class);

        return Map.of(
                "role", record.getUserType(),
                "profileComplete", record.getUserType() != null && profileName != null,
                "name", profileName != null ? profileName : record.getUsername()
        );
    }

    @Transactional(readOnly = true)
    public PageResult<UserSearchDto> searchUsers(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            return new PageResult<>(java.util.List.of(), page, size, 0);
        }
        return userRepository.searchUsers(query.trim(), page, size);
    }
}