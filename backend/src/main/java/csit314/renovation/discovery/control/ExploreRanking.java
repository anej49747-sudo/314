package csit314.renovation.discovery.control;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * General Explore discovery: recent (60%) + popular (40%), with a modest
 * non-personalised variety tie-break when similarly ranked posts are available.
 * No customer preferences, favourite similarity, or match scores are involved.
 */
public final class ExploreRanking {
    private ExploreRanking() {}

    public record Candidate(long id, long designerId, Long categoryId, long createdAt, long popularity) {}

    public static List<Candidate> order(List<Candidate> projects) {
        if (projects.size() < 2) return List.copyOf(projects);
        List<Candidate> newest = new ArrayList<>(projects);
        newest.sort(Comparator.comparingLong(Candidate::createdAt).reversed().thenComparingLong(Candidate::id));
        List<Candidate> popular = new ArrayList<>(projects);
        popular.sort(Comparator.comparingLong(Candidate::popularity).reversed().thenComparingLong(Candidate::id));
        Map<Long, Integer> timeRank = new HashMap<>();
        Map<Long, Integer> popularityRank = new HashMap<>();
        for (int i = 0; i < projects.size(); i++) {
            timeRank.put(newest.get(i).id(), i);
            popularityRank.put(popular.get(i).id(), i);
        }
        int maxIndex = projects.size() - 1;
        Map<Long, Double> scores = new HashMap<>();
        for (Candidate p : projects) {
            double time = 1.0 - (double) timeRank.get(p.id()) / maxIndex;
            double popularity = 1.0 - (double) popularityRank.get(p.id()) / maxIndex;
            scores.put(p.id(), .6 * time + .4 * popularity);
        }
        List<Candidate> remaining = new ArrayList<>(projects);
        remaining.sort(Comparator.comparingDouble((Candidate p) -> scores.get(p.id())).reversed()
                .thenComparingLong(Candidate::id));
        List<Candidate> result = new ArrayList<>(projects.size());
        while (!remaining.isEmpty()) {
            Candidate best = remaining.get(0);
            if (!result.isEmpty()) {
                Candidate previous = result.get(result.size() - 1);
                // Variety only breaks near-ties; don't push an unrelated low-ranked project ahead.
                for (int i = 1; i < remaining.size(); i++) {
                    Candidate alternate = remaining.get(i);
                    if (scores.get(best.id()) - scores.get(alternate.id()) > .08) break;
                    if (alternate.designerId() != previous.designerId()
                            && (best.designerId() == previous.designerId()
                                || (best.categoryId() != null && best.categoryId().equals(previous.categoryId())
                                && !best.categoryId().equals(alternate.categoryId())))) {
                        best = alternate;
                        break;
                    }
                }
            }
            result.add(best);
            remaining.remove(best);
        }
        return result;
    }
}
