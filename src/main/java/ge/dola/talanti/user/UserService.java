package ge.dola.talanti.user;

import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.user.dto.CompleteProfileDto;
import ge.dola.talanti.user.dto.PublicUserProfileDto;
import ge.dola.talanti.user.dto.UpdateCurrentUserProfileDto;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.user.event.UserFollowedEvent;
import ge.dola.talanti.user.event.UserRoleChangedEvent;
import ge.dola.talanti.user.repository.UserProfileRepository;
import ge.dola.talanti.user.repository.UserRepository;
import ge.dola.talanti.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static ge.dola.talanti.jooq.Tables.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final DSLContext dsl;
    private final ApplicationEventPublisher eventPublisher;
    private static final Set<UserType> ALLOWED_SELF_SELECTED_ROLES = Set.of(UserType.FAN, UserType.PLAYER);

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(cacheNames = "my-profile", key = "#userId")
    public void completeUserProfile(Long userId, CompleteProfileDto dto) {
        log.info("Initiating profile completion for User ID: {}", userId);

        // Fetch current role to determine state delta
        String oldRoleStr = dsl.select(USERS.USER_TYPE)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOneInto(String.class);
        if (oldRoleStr == null) {
            throw new ResourceNotFoundException("User not found.");
        }

        UserType oldRole = oldRoleStr != null ? UserType.valueOf(oldRoleStr) : null;
        UserType requestedRole = dto.role();
        UserType newRole;

        if (oldRole == UserType.CLUB_ADMIN || oldRole == UserType.SYSTEM_ADMIN) {
            newRole = oldRole;
        } else {
            if (!ALLOWED_SELF_SELECTED_ROLES.contains(requestedRole)) {
                throw new IllegalArgumentException("This role cannot be self-assigned.");
            }
            newRole = requestedRole;
        }

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
    public PublicUserProfileDto getProfile(Long targetUserId, Long currentUserId) {
        log.debug("User ID: {} fetching public profile for Target ID: {}", currentUserId, targetUserId);
        return userProfileRepository.getPublicProfile(targetUserId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User profile not found."));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(cacheNames = "my-profile", key = "#userId")
    public PublicUserProfileDto updateMyProfile(Long userId, UpdateCurrentUserProfileDto dto) {
        UserType currentRole = dsl.select(USERS.USER_TYPE)
                .from(USERS)
                .where(USERS.ID.eq(userId))
                .fetchOptional(record -> UserType.valueOf(record.get(USERS.USER_TYPE)))
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        dsl.insertInto(USER_PROFILES)
                .set(USER_PROFILES.USER_ID, userId)
                .onDuplicateKeyIgnore()
                .execute();

        Map<Field<?>, Object> profileUpdates = new LinkedHashMap<>();

        if (dto.fullName() != null) {
            profileUpdates.put(USER_PROFILES.FULL_NAME, dto.fullName());
        }
        if (dto.bio() != null) {
            profileUpdates.put(USER_PROFILES.BIO, dto.bio());
        }
        if (dto.avatarUrl() != null) {
            profileUpdates.put(USER_PROFILES.PROFILE_PICTURE_URL, dto.avatarUrl());
        }
        if (dto.bannerUrl() != null) {
            profileUpdates.put(USER_PROFILES.BANNER_URL, dto.bannerUrl());
        }

        if (!profileUpdates.isEmpty()) {
            dsl.update(USER_PROFILES)
                    .set(profileUpdates)
                    .where(USER_PROFILES.USER_ID.eq(userId))
                    .execute();
        }

        boolean touchesPlayerDetails =
                dto.position() != null
                        || dto.preferredFoot() != null
                        || dto.heightCm() != null
                        || dto.weightKg() != null;

        if (touchesPlayerDetails) {
            if (currentRole != UserType.PLAYER) {
                throw new IllegalArgumentException("Only players can update player-specific details.");
            }
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
        }

        return getProfile(userId, userId);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public boolean toggleFollow(Long targetUserId, Long currentUserId) {
        if (targetUserId.equals(currentUserId)) {
            log.warn("User ID: {} attempted to follow themselves. Blocked.", currentUserId);
            throw new IllegalArgumentException("You cannot follow yourself");
        }
        if (userRepository.findById(targetUserId).isEmpty()) {
            throw new ResourceNotFoundException("User not found.");
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
    @Cacheable(cacheNames = "my-profile", key = "#userId")
    public Map<String, Object> getMyProfileData(Long userId) {
        var summary = userRepository.findProfileSummary(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        String profileName = summary.fullName();
        boolean hasRealName = profileName != null
                && !profileName.isBlank()
                && !"New User".equalsIgnoreCase(profileName.trim());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", summary.id());
        response.put("username", summary.username());
        response.put("role", summary.role());
        response.put("fullName", profileName);
        response.put("profileComplete", summary.role() != null && hasRealName);
        response.put("name", hasRealName ? profileName : summary.username());
        return response;
    }

    @Transactional(readOnly = true)
    public PageResult<UserSearchDto> searchUsers(String query, int page, int size) {
        if (query == null || query.trim().length() < 2) {
            return new PageResult<>(java.util.List.of(), page, size, 0);
        }
        return userRepository.searchUsers(query.trim(), page, size);
    }
}
