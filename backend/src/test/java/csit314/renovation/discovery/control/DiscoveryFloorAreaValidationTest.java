package csit314.renovation.discovery.control;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import csit314.renovation.discovery.control.DiscoveryService.MetadataInput;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class DiscoveryFloorAreaValidationTest {
    @Test
    void invalidFloorAreaIsRejectedBeforeAnyDatabaseWrite() {
        for (String area : List.of("0", "-0.01", "12.345", "12.340", "0.001",
                "100000000", "99999999.999")) {
            NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
            DiscoveryService discovery = new DiscoveryService(db, null);
            MetadataInput input = new MetadataInput(null, new BigDecimal(area), null, List.of());

            assertThrows(IllegalArgumentException.class,
                    () -> discovery.saveMetadata(1L, input, false), area);
            verifyNoInteractions(db);
        }
    }

    @Test
    void validFloorAreaBoundariesAndOptionalNullAreAccepted() {
        for (String area : List.of("0.01", "48", "48.50", "99999999.99")) {
            NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
            DiscoveryService discovery = new DiscoveryService(db, null);

            assertDoesNotThrow(() -> discovery.saveMetadata(1L,
                    new MetadataInput(null, new BigDecimal(area), null, List.of()), false), area);
            verify(db, times(2)).update(anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>any());
        }
        NamedParameterJdbcTemplate db = mock(NamedParameterJdbcTemplate.class);
        DiscoveryService discovery = new DiscoveryService(db, null);
        assertDoesNotThrow(() -> discovery.saveMetadata(1L,
                new MetadataInput(null, null, null, List.of()), false));
        verify(db, times(2)).update(anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>any());
    }
}
