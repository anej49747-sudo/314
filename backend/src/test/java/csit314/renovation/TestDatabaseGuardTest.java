package csit314.renovation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TestDatabaseGuardTest {
    @Test
    void automaticallyAllowsTestDatabases() {
        assertTrue(TestDatabaseGuard.isAllowedDatabase("csit314_test", false));
    }

    @Test
    void requiresExplicitOptInForDisposableLocalDatabase() {
        assertFalse(TestDatabaseGuard.isAllowedDatabase("csit314_db", false));
        assertTrue(TestDatabaseGuard.isAllowedDatabase("csit314_db", true));
    }

    @Test
    void neverAllowsOtherDatabaseNames() {
        assertFalse(TestDatabaseGuard.isAllowedDatabase("production", true));
        assertFalse(TestDatabaseGuard.isAllowedDatabase(null, true));
    }
}
