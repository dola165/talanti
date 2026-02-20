package ge.dola.talanti.club;

import ge.dola.talanti.jooq.tables.records.ClubsRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.CLUBS;

@Repository
public class ClubRepository {

    private final DSLContext dsl;

    public ClubRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<ClubsRecord> findById(Long id) {
        return dsl.selectFrom(CLUBS)
                .where(CLUBS.ID.eq(id))
                .fetchOptional();
    }

    public List<ClubsRecord> findAll() {
        return dsl.selectFrom(CLUBS).fetch();
    }

    public ClubsRecord save(ClubsRecord club) {
        // JOOQ records can execute themselves if attached,
        // but explicit DSL execution is often cleaner.
        return dsl.insertInto(CLUBS)
                .set(club)
                .returning()
                .fetchOne();
    }
}