package ge.dola.talanti.security.authz;

import ge.dola.talanti.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

import static ge.dola.talanti.jooq.Tables.CLUB_MEMBERSHIPS;

@Component("clubAccessManager")
@RequiredArgsConstructor
public class ClubAccessManager implements DomainAccessManager<Long> {

    private final DSLContext dsl;

    @Override
    public boolean decide(CustomUserDetails user, Long clubId) {
        // Automatically grants access if they are an OWNER or CLUB_ADMIN for this specific club
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(CLUB_MEMBERSHIPS)
                        .where(CLUB_MEMBERSHIPS.CLUB_ID.eq(clubId))
                        .and(CLUB_MEMBERSHIPS.USER_ID.eq(user.getUserId()))
                        .and(CLUB_MEMBERSHIPS.ROLE.in("OWNER", "CLUB_ADMIN"))
        );
    }
}