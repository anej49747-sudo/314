package csit314.renovation.discovery.control;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationMathTest {
    @Test void favouriteInfluenceRampsThenCaps() {
        assertEquals(0,RecommendationMath.favouriteInfluence(0),1e-9);
        assertEquals(.08,RecommendationMath.favouriteInfluence(1),1e-9);
        assertEquals(.32,RecommendationMath.favouriteInfluence(4),1e-9);
        assertEquals(.40,RecommendationMath.favouriteInfluence(5),1e-9);
        assertEquals(.40,RecommendationMath.favouriteInfluence(100),1e-9);
    }
    @Test void blendRetainsExplicitMajority() {
        assertEquals(.6,RecommendationMath.blend(1.0,0.0,5),1e-9);
        assertEquals(.92,RecommendationMath.blend(1.0,0.0,1),1e-9);
    }
    @Test void missingDimensionsDoNotBecomePerfectMatches() {
        assertEquals(.5,RecommendationMath.preference(.25,.5),1e-9);
    }
    @Test void designerScoresUseTopThreeWithoutPortfolioSizeBonus() {
        assertEquals(.85,RecommendationMath.topThreeAverage(List.of(.92,.85,.78,.10)),1e-9);
        assertEquals(.9,RecommendationMath.topThreeAverage(List.of(.9)),1e-9);
        assertEquals(0,RecommendationMath.topThreeAverage(List.of()),1e-9);
    }
}
