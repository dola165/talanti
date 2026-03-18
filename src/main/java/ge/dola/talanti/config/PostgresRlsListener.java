package ge.dola.talanti.config;

import ge.dola.talanti.security.CustomUserDetails;
import ge.dola.talanti.security.util.SecurityUtils;
import org.jooq.ExecuteContext;
import org.jooq.ExecuteListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.Statement;
import java.util.Optional;

@Component
public class PostgresRlsListener implements ExecuteListener {

    @Override
    public void start(ExecuteContext ctx) {
        // IMMUNITY FIX: If there is no active Spring transaction, do not attempt SET LOCAL.
        // This prevents Postgres from severing the connection on read-only auto-commit queries.
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }

        Optional<CustomUserDetails> currentUserOpt = SecurityUtils.getCurrentUser();

        try (Statement stmt = ctx.connection().createStatement()) {
            if (currentUserOpt.isPresent()) {
                stmt.execute("SET LOCAL talanti.current_user_id = '" + currentUserOpt.get().getUserId() + "'");
            } else {
                stmt.execute("SET LOCAL talanti.current_user_id = ''");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to enforce database Row-Level Security context", e);
        }
    }
}