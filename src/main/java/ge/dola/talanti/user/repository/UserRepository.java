package ge.dola.talanti.user.repository;

import ge.dola.talanti.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static ge.dola.talanti.jooq.Tables.USERS;

@Repository
public class UserRepository {

    private final DSLContext dsl;

    public UserRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public Optional<UsersRecord> findByEmail(String email) {
        return dsl.selectFrom(USERS)
                .where(USERS.EMAIL.eq(email))
                .fetchOptional();
    }

    public Optional<UsersRecord> findById(Long id) {
        return dsl.selectFrom(USERS)
                .where(USERS.ID.eq(id))
                .fetchOptional();
    }

    // You can add more JOOQ queries here later, like save() or update()
}