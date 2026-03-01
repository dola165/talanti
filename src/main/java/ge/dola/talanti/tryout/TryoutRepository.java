package ge.dola.talanti.tryout;

import ge.dola.talanti.tryout.dto.TryoutMapDto;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static ge.dola.talanti.jooq.Tables.*;

@Repository
@RequiredArgsConstructor
public class TryoutRepository {

    private final DSLContext dsl;

    public List<TryoutMapDto> getUpcomingTryoutsForMap() {
        return dsl.select(
                        TRYOUTS.ID,
                        CLUBS.ID.as("clubId"),
                        CLUBS.NAME.as("clubName"),
                        TRYOUTS.TITLE,
                        TRYOUTS.DESCRIPTION,
                        TRYOUTS.POSITION,
                        TRYOUTS.AGE_GROUP,
                        TRYOUTS.TRYOUT_DATE,
                        LOCATIONS.LATITUDE,
                        LOCATIONS.LONGITUDE,
                        LOCATIONS.ADDRESS_TEXT
                )
                .from(TRYOUTS)
                .join(CLUBS).on(TRYOUTS.CLUB_ID.eq(CLUBS.ID))
                // Inner join ensures we only get tryouts that actually have a physical location pinned
                .join(LOCATIONS).on(TRYOUTS.LOCATION_ID.eq(LOCATIONS.ID))
                .where(TRYOUTS.TRYOUT_DATE.greaterOrEqual(LocalDateTime.now()))
                .orderBy(TRYOUTS.TRYOUT_DATE.asc())
                .fetchInto(TryoutMapDto.class); // JOOQ magically maps the columns to your Record!
    }
}