package csit314.renovation;

import org.springframework.jdbc.core.JdbcTemplate;

/** Guards the integration tests that deliberately delete account records. Test sources only. */
public final class TestDatabaseGuard {
    private TestDatabaseGuard() {}

    public static void requireSafeForDestructiveTests(JdbcTemplate jdbc) {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        boolean allowLocalReset = "true".equalsIgnoreCase(System.getenv("ALLOW_DEV_DB_TEST_RESET"));
        if (!isAllowedDatabase(database, allowLocalReset)) {
            throw new IllegalStateException("Refusing destructive test cleanup on database: " + database
                    + ". For disposable local csit314_db, set ALLOW_DEV_DB_TEST_RESET=true.");
        }
    }

    static boolean isAllowedDatabase(String database, boolean allowLocalReset) {
        return database != null && (database.endsWith("_test")
                || (allowLocalReset && database.equals("csit314_db")));
    }
}
