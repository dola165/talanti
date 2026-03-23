package ge.dola.talanti.club.management;

import ge.dola.talanti.club.ClubDynamicTables;
import ge.dola.talanti.club.ClubMembershipRepository;
import ge.dola.talanti.club.ClubRole;
import ge.dola.talanti.config.ResourceNotFoundException;
import ge.dola.talanti.notification.NotificationService;
import ge.dola.talanti.club.management.dto.ClubManagedMemberDto;
import ge.dola.talanti.club.management.dto.ClubManagementOverviewDto;
import ge.dola.talanti.club.management.dto.ClubMembershipInviteDto;
import ge.dola.talanti.club.management.dto.CreateClubInviteDto;
import ge.dola.talanti.club.management.dto.CreateHonourDto;
import ge.dola.talanti.club.management.dto.CreateOpportunityDto;
import ge.dola.talanti.club.management.dto.UpdateClubMemberRoleDto;
import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.security.util.SecurityUtils;
import ge.dola.talanti.user.dto.UserSearchDto;
import ge.dola.talanti.user.repository.UserRepository;
import ge.dola.talanti.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import static ge.dola.talanti.jooq.Tables.CLUBS;
import static ge.dola.talanti.jooq.Tables.CLUB_HONOURS;
import static ge.dola.talanti.jooq.Tables.CLUB_OPPORTUNITIES;
import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;
import static ge.dola.talanti.jooq.Tables.USER_PROFILES;
import static ge.dola.talanti.jooq.Tables.USERS;
import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubManagementService {

    private final DSLContext dsl;
    private final ClubMembershipRepository clubMembershipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public ClubManagementOverviewDto getManagementOverview(Long clubId) {
        ActingAccess actingAccess = resolveActingAccess(clubId);

        var members = dsl.select(
                        USERS.ID.as("userId"),
                        USER_PROFILES.FULL_NAME.as("fullName"),
                        USERS.USERNAME,
                        USER_PROFILES.PROFILE_PICTURE_URL.as("avatarUrl"),
                        CLUB_MEMBERSHIPS.ROLE
                )
                .from(CLUB_MEMBERSHIPS)
                .join(USERS).on(USERS.ID.eq(CLUB_MEMBERSHIPS.USER_ID))
                .leftJoin(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                .orderBy(
                        DSL.when(CLUB_MEMBERSHIPS.ROLE.eq(ClubRole.OWNER.name()), 0)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq(ClubRole.CLUB_ADMIN.name()), 1)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq(ClubRole.COACH.name()), 2)
                                .when(CLUB_MEMBERSHIPS.ROLE.eq(ClubRole.AGENT.name()), 3)
                                .otherwise(4)
                                .asc(),
                        USER_PROFILES.FULL_NAME.asc().nullsLast(),
                        USERS.USERNAME.asc(),
                        USERS.ID.asc()
                )
                .fetch(record -> {
                    ClubRole memberRole = ClubRole.from(record.get(CLUB_MEMBERSHIPS.ROLE));
                    Long memberUserId = record.get("userId", Long.class);
                    return new ClubManagedMemberDto(
                            memberUserId,
                            record.get("fullName", String.class),
                            record.get(USERS.USERNAME),
                            record.get("avatarUrl", String.class),
                            memberRole.name(),
                            canEditRole(actingAccess, memberUserId, memberRole)
                    );
                });

        var invitations = dsl.select(
                        ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ID.as("id"),
                        ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITEE_USER_ID.as("userId"),
                        USER_PROFILES.FULL_NAME.as("fullName"),
                        USERS.USERNAME,
                        USER_PROFILES.PROFILE_PICTURE_URL.as("avatarUrl"),
                        ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ROLE.as("role"),
                        ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS.as("status"),
                        ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CREATED_AT.as("createdAt")
                )
                .from(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES)
                .join(USERS).on(USERS.ID.eq(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITEE_USER_ID))
                .leftJoin(USER_PROFILES).on(USER_PROFILES.USER_ID.eq(USERS.ID))
                .where(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CLUB_ID.eq(clubId))
                .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS.eq("PENDING"))
                .orderBy(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CREATED_AT.desc(), ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ID.desc())
                .fetchInto(ClubMembershipInviteDto.class);

        return new ClubManagementOverviewDto(
                actingAccess.displayRole(),
                actingAccess.assignableRoles().stream().map(Enum::name).toList(),
                members,
                invitations
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public PageResult<UserSearchDto> searchUsersForInvite(Long clubId, String query, int page, int size) {
        ActingAccess actingAccess = resolveActingAccess(clubId);
        return userRepository.searchUsersForClubInvite(clubId, actingAccess.userId(), query, page, size);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public Long createInvitation(Long clubId, CreateClubInviteDto dto) {
        ActingAccess actingAccess = resolveActingAccess(clubId);
        ClubRole requestedRole = ClubRole.from(dto.role());

        assertRoleAssignable(actingAccess, requestedRole);
        if (dto.userId().equals(actingAccess.userId())) {
            throw new AccessDeniedException("You cannot invite yourself.");
        }
        if (clubMembershipRepository.findRole(clubId, dto.userId()).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this club.");
        }
        var targetUser = userRepository.findById(dto.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        if ("SYSTEM_ADMIN".equalsIgnoreCase(targetUser.getUserType())) {
            throw new AccessDeniedException("System administrators cannot be invited to a club role.");
        }
        if (hasPendingInvitation(clubId, dto.userId())) {
            throw new IllegalArgumentException("This user already has a pending invitation.");
        }

        log.info("Club ID [{}] inviting User ID [{}] into role [{}]", safe(clubId), safe(dto.userId()), safe(requestedRole));

        Long inviteId = dsl.insertInto(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES)
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CLUB_ID, clubId)
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITEE_USER_ID, dto.userId())
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ROLE, requestedRole.name())
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS, "PENDING")
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITED_BY, actingAccess.userId())
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CREATED_AT, LocalDateTime.now())
                .returningResult(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ID)
                .fetchOneInto(Long.class);

        notificationService.notifyClubInvitationReceived(dto.userId(), inviteId, clubId, requestedRole);
        return inviteId;
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void updateMemberRole(Long clubId, Long targetUserId, UpdateClubMemberRoleDto dto) {
        ActingAccess actingAccess = resolveActingAccess(clubId);
        ClubRole existingRole = clubMembershipRepository.findRole(clubId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Club member not found."));
        ClubRole requestedRole = ClubRole.from(dto.role());

        assertRoleUpdateAllowed(actingAccess, targetUserId, existingRole, requestedRole);

        log.info("Club ID [{}] changing User ID [{}] role from [{}] to [{}]",
                safe(clubId),
                safe(targetUserId),
                safe(existingRole),
                safe(requestedRole));

        clubMembershipRepository.updateMembershipRole(clubId, targetUserId, requestedRole);
        if (existingRole != requestedRole) {
            notificationService.notifyClubRoleChanged(targetUserId, clubId, requestedRole);
        }
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void cancelInvitation(Long clubId, Long inviteId) {
        resolveActingAccess(clubId);

        int updated = dsl.update(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES)
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS, "CANCELLED")
                .set(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_RESPONDED_AT, LocalDateTime.now())
                .where(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_ID.eq(inviteId))
                .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CLUB_ID.eq(clubId))
                .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS.eq("PENDING"))
                .execute();

        if (updated == 0) {
            throw new ResourceNotFoundException("Pending invitation not found for this club.");
        }
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public Long addOpportunity(Long clubId, CreateOpportunityDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String normalizedType = dto.type().trim().toUpperCase();
        String normalizedTitle = dto.title().trim();
        String normalizedDescription = dto.description() == null ? null : dto.description().trim();
        String normalizedExternalLink = dto.externalLink() == null || dto.externalLink().isBlank() ? null : dto.externalLink().trim();
        log.info("Admin ID [{}] adding {} opportunity to Club ID [{}]", safe(currentUserId), safe(normalizedType), safe(clubId));

        return dsl.insertInto(CLUB_OPPORTUNITIES)
                .set(CLUB_OPPORTUNITIES.CLUB_ID, clubId)
                .set(CLUB_OPPORTUNITIES.TYPE, normalizedType)
                .set(CLUB_OPPORTUNITIES.TITLE, normalizedTitle)
                .set(CLUB_OPPORTUNITIES.DESCRIPTION, normalizedDescription)
                .set(CLUB_OPPORTUNITIES.EXTERNAL_LINK, normalizedExternalLink)
                .set(CLUB_OPPORTUNITIES.STATUS, "OPEN")
                .set(CLUB_OPPORTUNITIES.CREATED_AT, java.time.LocalDateTime.now())
                .returningResult(CLUB_OPPORTUNITIES.ID)
                .fetchOneInto(Long.class);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void deleteOpportunity(Long clubId, Long opportunityId) {
        log.info("Admin ID [{}] deleting opportunity [{}] from Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(opportunityId), safe(clubId));
        int deleted = dsl.deleteFrom(CLUB_OPPORTUNITIES)
                .where(CLUB_OPPORTUNITIES.ID.eq(opportunityId))
                .and(CLUB_OPPORTUNITIES.CLUB_ID.eq(clubId)) // Strict scoping
                .execute();
        if (deleted == 0) {
            throw new ResourceNotFoundException("Opportunity not found for this club.");
        }
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public Long addHonour(Long clubId, CreateHonourDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Admin ID [{}] adding honour to Club ID [{}]", safe(currentUserId), safe(clubId));
        String normalizedTitle = dto.title().trim();
        String normalizedDescription = dto.description() == null ? null : dto.description().trim();

        return dsl.insertInto(CLUB_HONOURS)
                .set(CLUB_HONOURS.CLUB_ID, clubId)
                .set(CLUB_HONOURS.TITLE, normalizedTitle)
                .set(CLUB_HONOURS.YEAR_WON, dto.yearWon())
                .set(CLUB_HONOURS.DESCRIPTION, normalizedDescription)
                .returningResult(CLUB_HONOURS.ID)
                .fetchOneInto(Long.class);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void deleteHonour(Long clubId, Long honourId) {
        log.info("Admin ID [{}] deleting honour [{}] from Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(honourId), safe(clubId));
        int deleted = dsl.deleteFrom(CLUB_HONOURS)
                .where(CLUB_HONOURS.ID.eq(honourId))
                .and(CLUB_HONOURS.CLUB_ID.eq(clubId)) // Strict scoping
                .execute();
        if (deleted == 0) {
            throw new ResourceNotFoundException("Honour not found for this club.");
        }
    }

    private ActingAccess resolveActingAccess(Long clubId) {
        if (!dsl.fetchExists(dsl.selectOne().from(CLUBS).where(CLUBS.ID.eq(clubId)))) {
            throw new ResourceNotFoundException("Club not found.");
        }

        CustomUserDetails currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in context"));

        if (currentUser.hasRole("SYSTEM_ADMIN")) {
            return new ActingAccess(currentUser.getUserId(), null, true);
        }

        ClubRole clubRole = clubMembershipRepository.findRole(clubId, currentUser.getUserId())
                .orElseThrow(() -> new AccessDeniedException("You do not have access to this club."));

        return new ActingAccess(currentUser.getUserId(), clubRole, false);
    }

    private boolean hasPendingInvitation(Long clubId, Long userId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES)
                        .where(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_CLUB_ID.eq(clubId))
                        .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_INVITEE_USER_ID.eq(userId))
                        .and(ClubDynamicTables.CLUB_MEMBERSHIP_INVITES_STATUS.eq("PENDING"))
        );
    }

    private void assertRoleAssignable(ActingAccess actingAccess, ClubRole requestedRole) {
        if (requestedRole == ClubRole.OWNER) {
            throw new AccessDeniedException("Owner role cannot be assigned from this flow.");
        }
        if (actingAccess.systemAdmin()) {
            return;
        }
        if (!actingAccess.clubRole().canAssignRole(requestedRole)) {
            throw new AccessDeniedException("You do not have permission to assign this role.");
        }
    }

    private void assertRoleUpdateAllowed(ActingAccess actingAccess,
                                         Long targetUserId,
                                         ClubRole existingRole,
                                         ClubRole requestedRole) {
        if (targetUserId.equals(actingAccess.userId())) {
            throw new AccessDeniedException("You cannot change your own club role.");
        }
        if (existingRole == ClubRole.OWNER || requestedRole == ClubRole.OWNER) {
            throw new AccessDeniedException("Owner role cannot be changed from this flow.");
        }
        assertRoleAssignable(actingAccess, requestedRole);
        if (!actingAccess.systemAdmin()
                && actingAccess.clubRole() == ClubRole.CLUB_ADMIN
                && existingRole == ClubRole.CLUB_ADMIN) {
            throw new AccessDeniedException("Club admins cannot change another club admin.");
        }
    }

    private boolean canEditRole(ActingAccess actingAccess, Long memberUserId, ClubRole memberRole) {
        if (memberUserId.equals(actingAccess.userId())) {
            return false;
        }
        if (memberRole == ClubRole.OWNER) {
            return false;
        }
        if (actingAccess.systemAdmin()) {
            return true;
        }
        if (actingAccess.clubRole() == ClubRole.OWNER) {
            return true;
        }
        return actingAccess.clubRole() == ClubRole.CLUB_ADMIN && memberRole != ClubRole.CLUB_ADMIN;
    }

    private record ActingAccess(Long userId, ClubRole clubRole, boolean systemAdmin) {
        List<ClubRole> assignableRoles() {
            if (systemAdmin) {
                return List.of(ClubRole.CLUB_ADMIN, ClubRole.COACH, ClubRole.AGENT, ClubRole.PLAYER);
            }
            return clubRole.assignableRoles();
        }

        String displayRole() {
            return systemAdmin ? "SYSTEM_ADMIN" : clubRole.name();
        }
    }
}
