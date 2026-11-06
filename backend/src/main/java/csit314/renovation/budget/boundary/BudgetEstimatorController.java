package csit314.renovation.budget.boundary;

import csit314.renovation.budget.control.BudgetEstimationService;
import csit314.renovation.budget.dto.BudgetEstimateRequest;
import csit314.renovation.budget.dto.BudgetEstimateResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/budget")
public class BudgetEstimatorController {
    private final BudgetEstimationService service;

    public BudgetEstimatorController(BudgetEstimationService service) {
        this.service = service;
    }

    @PostMapping("/estimate")
    public BudgetEstimateResponse estimate(@RequestBody BudgetEstimateRequest request) {
        return service.estimate(request);
    }
}
