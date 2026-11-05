package csit314.renovation.budget.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Customer choices only. The server owns all example rates and adjustment factors. */
public record BudgetEstimateRequest(
        String propertyType,
        String propertyCondition,
        BigDecimal floorAreaSqm,
        Map<String, String> roomIntensities,
        List<String> workTypes,
        Integer contingencyPercent) {
}
