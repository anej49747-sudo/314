package csit314.renovation.discovery.control;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ExploreRankingTest {
    private static ExploreRanking.Candidate p(long id,long designer,long category,long created,long popularity) {
        return new ExploreRanking.Candidate(id,designer,category,created,popularity);
    }
    @Test void emptyAndSingleProjectAreSupported() {
        assertTrue(ExploreRanking.order(List.of()).isEmpty());
        assertEquals(List.of(1L), ExploreRanking.order(List.of(p(1,10,5,100,0))).stream().map(ExploreRanking.Candidate::id).toList());
    }
    @Test void generalFeedBalancesRecencyAndPopularity() {
        var projects=List.of(p(1,1,1,300,1),p(2,2,2,200,100),p(3,3,3,100,0));
        var ids=ExploreRanking.order(projects).stream().map(ExploreRanking.Candidate::id).toList();
        assertEquals(3, ids.size());
        assertNotEquals(3L,ids.get(0)); // old and unpopular is not ranked first
        assertEquals(ids,ExploreRanking.order(projects).stream().map(ExploreRanking.Candidate::id).toList());
    }
    @Test void doesNotDependOnCustomerSignalsAndRetainsEveryProject() {
        var projects=List.of(p(3,7,2,20,0),p(2,7,2,21,10),p(1,8,3,22,10));
        var ordered=ExploreRanking.order(projects);
        assertEquals(3,ordered.size());
        assertEquals(java.util.Set.of(1L,2L,3L),
                ordered.stream().map(ExploreRanking.Candidate::id).collect(java.util.stream.Collectors.toSet()));
    }
}
