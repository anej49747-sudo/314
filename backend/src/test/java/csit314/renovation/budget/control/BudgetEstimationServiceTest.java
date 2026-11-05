package csit314.renovation.budget.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import csit314.renovation.budget.dto.BudgetEstimateRequest;
import csit314.renovation.budget.dto.BudgetEstimateResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BudgetEstimationServiceTest {
    private final BudgetEstimationService service = new BudgetEstimationService();

    private BudgetEstimateRequest typical() {
        return new BudgetEstimateRequest("hdb", "resale", new BigDecimal("95"),
                Map.of("living", "standard", "kitchen", "extensive",
                        "bedrooms", "light", "bathrooms", "standard"),
                List.of("painting", "flooring", "carpentry", "electrical"), 10);
    }

    @Test
    void defaultMatchesApprovedV3IndicativeBreakdown() {
        BudgetEstimateResponse result = service.estimate(typical());
        assertEquals(4, result.rooms().size());
        assertEquals(4, result.workRates().size());
        assertEquals(new BigDecimal("1.12"), result.conditionFactor());
        // Display midpoint rounds to the nearest S$100 in the frontend.
        assertEquals(0, result.midpoint().compareTo(new BigDecimal("48519.17")),
                "Update this assertion only after checking the V3 calculation reference.");
        assertEquals(10, result.contingencyPercent());
        assertTrue(result.rangeLow().compareTo(result.midpoint()) < 0);
        assertTrue(result.rangeHigh().compareTo(result.midpoint()) > 0);
    }

    @Test
    void contingencyIncreasesTotalWithoutChangingBase() {
        BudgetEstimateRequest base = typical();
        BudgetEstimateResponse zero = service.estimate(new BudgetEstimateRequest(
                base.propertyType(), base.propertyCondition(), base.floorAreaSqm(),
                base.roomIntensities(), base.workTypes(), 0));
        BudgetEstimateResponse thirty = service.estimate(new BudgetEstimateRequest(
                base.propertyType(), base.propertyCondition(), base.floorAreaSqm(),
                base.roomIntensities(), base.workTypes(), 30));
        assertEquals(zero.baseLow(), thirty.baseLow());
        assertEquals(zero.baseHigh(), thirty.baseHigh());
        assertEquals(zero.baseLow(), zero.rangeLow());
        assertEquals(0, thirty.rangeLow().compareTo(zero.rangeLow().multiply(new BigDecimal("1.30"))
                .setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void selectedRoomsAndWorksAffectCostsAndInputOrderDoesNot() {
        BudgetEstimateResponse kitchen = service.estimate(new BudgetEstimateRequest(
                "hdb", "new", new BigDecimal("80"), Map.of("kitchen", "standard"),
                List.of("painting"), 0));
        BudgetEstimateResponse kitchenMoreWork = service.estimate(new BudgetEstimateRequest(
                "hdb", "new", new BigDecimal("80"), Map.of("kitchen", "standard"),
                List.of("electrical", "painting"), 0));
        assertEquals(1, kitchen.rooms().size());
        assertTrue(kitchenMoreWork.rangeLow().compareTo(kitchen.rangeLow()) > 0);
        BudgetEstimateResponse ordered = service.estimate(new BudgetEstimateRequest(
                "hdb", "new", new BigDecimal("80"), Map.of("kitchen", "standard"),
                List.of("painting", "electrical"), 0));
        assertEquals(ordered, kitchenMoreWork);
    }

    @Test
    void rejectsMissingInvalidOrManipulatedValues() {
        BudgetEstimateRequest original = typical();
        for (BudgetEstimateRequest invalid : List.of(
                new BudgetEstimateRequest(null, "resale", original.floorAreaSqm(), original.roomIntensities(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", null, original.floorAreaSqm(), original.roomIntensities(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", new BigDecimal("9.99"), original.roomIntensities(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", new BigDecimal("1000.01"), original.roomIntensities(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", new BigDecimal("95.001"), original.roomIntensities(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), Map.of(), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), Map.of("kitchen", "unlimited"), original.workTypes(), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), original.roomIntensities(), List.of(), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), original.roomIntensities(), List.of("painting", "painting"), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), original.roomIntensities(), List.of("demo-rate"), 10),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), original.roomIntensities(), original.workTypes(), 7),
                new BudgetEstimateRequest("hdb", "resale", original.floorAreaSqm(), original.roomIntensities(), original.workTypes(), 35))) {
            assertThrows(IllegalArgumentException.class, () -> service.estimate(invalid));
        }
    }
}
