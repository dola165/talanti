package ge.dola.talanti.club.management;

import ge.dola.talanti.club.management.dto.CreateHonourDto;
import ge.dola.talanti.club.management.dto.CreateOpportunityDto;
import ge.dola.talanti.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static ge.dola.talanti.jooq.Tables.CLUB_HONOURS;
import static ge.dola.talanti.jooq.Tables.CLUB_OPPORTUNITIES;
import static ge.dola.talanti.security.util.LogSafe.safe;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClubManagementService {

    private final DSLContext dsl;

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public Long addOpportunity(Long clubId, CreateOpportunityDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Admin ID [{}] adding {} opportunity to Club ID [{}]", safe(currentUserId), safe(dto.type()), safe(clubId));

        return dsl.insertInto(CLUB_OPPORTUNITIES)
                .set(CLUB_OPPORTUNITIES.CLUB_ID, clubId)
                .set(CLUB_OPPORTUNITIES.TYPE, dto.type())
                .set(CLUB_OPPORTUNITIES.TITLE, dto.title())
                .set(CLUB_OPPORTUNITIES.DESCRIPTION, dto.description())
                .set(CLUB_OPPORTUNITIES.EXTERNAL_LINK, dto.externalLink())
                .set(CLUB_OPPORTUNITIES.STATUS, "OPEN")
                .set(CLUB_OPPORTUNITIES.CREATED_AT, java.time.LocalDateTime.now())
                .returningResult(CLUB_OPPORTUNITIES.ID)
                .fetchOneInto(Long.class);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void deleteOpportunity(Long clubId, Long opportunityId) {
        log.info("Admin ID [{}] deleting opportunity [{}] from Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(opportunityId), safe(clubId));
        dsl.deleteFrom(CLUB_OPPORTUNITIES)
                .where(CLUB_OPPORTUNITIES.ID.eq(opportunityId))
                .and(CLUB_OPPORTUNITIES.CLUB_ID.eq(clubId)) // Strict scoping
                .execute();
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public Long addHonour(Long clubId, CreateHonourDto dto) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Admin ID [{}] adding honour to Club ID [{}]", safe(currentUserId), safe(clubId));

        return dsl.insertInto(CLUB_HONOURS)
                .set(CLUB_HONOURS.CLUB_ID, clubId)
                .set(CLUB_HONOURS.TITLE, dto.title())
                .set(CLUB_HONOURS.YEAR_WON, dto.yearWon())
                .set(CLUB_HONOURS.DESCRIPTION, dto.description())
                .returningResult(CLUB_HONOURS.ID)
                .fetchOneInto(Long.class);
    }

    @Transactional
    @PreAuthorize("@clubAccessManager.decide(principal, #clubId)")
    public void deleteHonour(Long clubId, Long honourId) {
        log.info("Admin ID [{}] deleting honour [{}] from Club ID [{}]", safe(SecurityUtils.getCurrentUserId()), safe(honourId), safe(clubId));
        dsl.deleteFrom(CLUB_HONOURS)
                .where(CLUB_HONOURS.ID.eq(honourId))
                .and(CLUB_HONOURS.CLUB_ID.eq(clubId)) // Strict scoping
                .execute();
    }
}