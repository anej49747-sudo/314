package csit314.renovation.budget.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetEstimateResponse(
        BigDecimal midpoint,
        BigDecimal rangeLow,
        BigDecimal rangeHigh,
        BigDecimal baseLow,
        BigDecimal baseHigh,
        BigDecimal contingencyLow,
        BigDecimal contingencyHigh,
        int contingencyPercent,
        String propertyType,
        String propertyCondition,
        BigDecimal floorAreaSqm,
        BigDecimal propertyFactor,
        BigDecimal conditionFactor,
        List<RoomCost> rooms,
        List<WorkRate> workRates) {

    public record RoomCost(String id, String name, String intensity, BigDecimal areaSqm,
            BigDecimal baseLow, BigDecimal baseHigh) {
    }

    public record WorkRate(String id, String name, BigDecimal rateLow, BigDecimal rateHigh) {
    }
}
