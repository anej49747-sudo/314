package csit314.renovation.discovery.control;

import java.util.Collection;
import java.util.Comparator;

/** Explainable, deterministic content-based scoring for Sprint 4 #23/#24. */
public final class RecommendationMath {
    private RecommendationMath() {}
    public static double preference(double weightedSum,double selectedWeight) {
        return selectedWeight == 0 ? 0.5 : weightedSum / selectedWeight;
    }
    /** 8% of influence per informative favourite; never more than 40% with explicit interests. */
    public static double favouriteInfluence(int count) { return Math.min(.40, Math.max(0,count)*.08); }
    public static double blend(double explicit,double inferred,int favouriteCount) {
        double a=favouriteInfluence(favouriteCount);
        return (1-a)*explicit + a*inferred;
    }
    /** A designer with one or two projects uses those projects, not imaginary zero scores. */
    public static double topThreeAverage(Collection<Double> projectScores) {
        return projectScores.stream().sorted(Comparator.reverseOrder()).limit(3)
                .mapToDouble(Double::doubleValue).average().orElse(0);
    }
}
