package csit314.renovation.budget.control;

import csit314.renovation.budget.dto.BudgetEstimateRequest;
import csit314.renovation.budget.dto.BudgetEstimateResponse;
import csit314.renovation.budget.dto.BudgetEstimateResponse.RoomCost;
import csit314.renovation.budget.dto.BudgetEstimateResponse.WorkRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Story #27: a reproducible planning model, NOT a Singapore market-price quotation.
 * The illustrative V3 mockup values remain server-side until research-backed rates
 * and room assumptions are separately approved.
 */
@Service
public class BudgetEstimationService {
    private static final BigDecimal MIN_AREA = bd("10");
    private static final BigDecimal MAX_AREA = bd("1000");
    private static final BigDecimal HUNDRED = bd("100");
    private static final BigDecimal HALF = bd("2");

    private record Room(String id, String name, BigDecimal share, BigDecimal multiplier) {}
    private record Rate(String id, String name, BigDecimal low, BigDecimal high) {}

    private static final List<Room> ROOMS = List.of(
            new Room("living", "Living & dining", bd("0.35"), bd("1")),
            new Room("kitchen", "Kitchen", bd("0.20"), bd("1.25")),
            new Room("bedrooms", "Bedrooms", bd("0.30"), bd("0.90")),
            new Room("bathrooms", "Bathrooms", bd("0.15"), bd("1.35")));

    private static final List<Rate> WORKS = List.of(
            new Rate("painting", "Painting", bd("45"), bd("55")),
            new Rate("flooring", "Flooring", bd("83"), bd("102")),
            new Rate("carpentry", "Carpentry", bd("149"), bd("182")),
            new Rate("electrical", "Electrical work", bd("61"), bd("74")));

    private static final Map<String, BigDecimal> INTENSITIES = Map.of(
            "light", bd("0.7"), "standard", bd("1"), "extensive", bd("1.45"));
    private static final Map<String, BigDecimal> PROPERTY_FACTORS = Map.of(
            "hdb", bd("1"), "condo", bd("1.10"), "landed", bd("1.25"));

    public BudgetEstimateResponse estimate(BudgetEstimateRequest request) {
        if (request == null) throw invalid("Provide renovation details to calculate an estimate.");
        BigDecimal propertyFactor = request.propertyType() == null ? null : PROPERTY_FACTORS.get(request.propertyType());
        if (propertyFactor == null) throw invalid("Choose a valid property type.");
        if (!"new".equals(request.propertyCondition()) && !"resale".equals(request.propertyCondition())) {
            throw invalid("Choose a valid property condition.");
        }
        BigDecimal conditionFactor = "resale".equals(request.propertyCondition()) ? bd("1.12") : BigDecimal.ONE;
        BigDecimal area = request.floorAreaSqm();
        if (area == null || area.compareTo(MIN_AREA) < 0 || area.compareTo(MAX_AREA) > 0
                || area.scale() > 2) {
            throw invalid("Enter 10 to 1,000 m² using no more than two decimal places.");
        }
        Integer contingency = request.contingencyPercent();
        if (contingency == null || contingency < 0 || contingency > 30 || contingency % 5 != 0) {
            throw invalid("Contingency must be 0–30% in increments of 5%.");
        }
        Map<String, String> chosenRooms = request.roomIntensities();
        if (chosenRooms == null || chosenRooms.isEmpty()) {
            throw invalid("Select at least one renovation area.");
        }
        for (var choice : chosenRooms.entrySet()) {
            if (ROOMS.stream().noneMatch(room -> room.id().equals(choice.getKey()))
                    || choice.getValue() == null || !INTENSITIES.containsKey(choice.getValue())) {
                throw invalid("Choose a valid renovation area and work intensity.");
            }
        }
        List<String> selectedWorks = request.workTypes();
        if (selectedWorks == null || selectedWorks.isEmpty()) {
            throw invalid("Select at least one type of work.");
        }
        if (new LinkedHashSet<>(selectedWorks).size() != selectedWorks.size()
                || selectedWorks.stream().anyMatch(id -> WORKS.stream().noneMatch(work -> work.id().equals(id)))) {
            throw invalid("Choose valid, distinct work types.");
        }
        // Iterate catalogue order, not user-provided input order, for reproducible results.
        List<Rate> rates = WORKS.stream().filter(work -> selectedWorks.contains(work.id())).toList();
        BigDecimal lowRate = rates.stream().map(Rate::low).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal highRate = rates.stream().map(Rate::high).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RoomCost> breakdown = new ArrayList<>();
        BigDecimal lowBase = BigDecimal.ZERO;
        BigDecimal highBase = BigDecimal.ZERO;
        for (Room room : ROOMS) {
            String intensity = chosenRooms.get(room.id());
            if (intensity == null) continue;
            BigDecimal roomArea = area.multiply(room.share());
            BigDecimal factor = roomArea.multiply(INTENSITIES.get(intensity))
                    .multiply(room.multiplier()).multiply(propertyFactor).multiply(conditionFactor);
            BigDecimal roomLow = factor.multiply(lowRate);
            BigDecimal roomHigh = factor.multiply(highRate);
            lowBase = lowBase.add(roomLow);
            highBase = highBase.add(roomHigh);
            breakdown.add(new RoomCost(room.id(), room.name(), intensity, money(roomArea),
                    money(roomLow), money(roomHigh)));
        }
        BigDecimal contingencyFactor = BigDecimal.valueOf(contingency).divide(HUNDRED);
        BigDecimal lowContingency = lowBase.multiply(contingencyFactor);
        BigDecimal highContingency = highBase.multiply(contingencyFactor);
        BigDecimal totalLow = lowBase.add(lowContingency);
        BigDecimal totalHigh = highBase.add(highContingency);
        return new BudgetEstimateResponse(
                money(totalLow.add(totalHigh).divide(HALF)), money(totalLow), money(totalHigh),
                money(lowBase), money(highBase), money(lowContingency), money(highContingency),
                contingency, request.propertyType(), request.propertyCondition(), area, propertyFactor,
                conditionFactor, List.copyOf(breakdown), rates.stream()
                        .map(work -> new WorkRate(work.id(), work.name(), work.low(), work.high())).toList());
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }
}
