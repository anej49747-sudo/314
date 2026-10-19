package csit314.renovation.portfolio.boundary;

import csit314.renovation.portfolio.control.PortfolioService;
import csit314.renovation.portfolio.dto.PortfolioProjectResponse;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/designer/projects")
public class PortfolioController {
    private final PortfolioService portfolios;

    public PortfolioController(PortfolioService portfolios) {
        this.portfolios = portfolios;
    }

    @GetMapping
    public List<PortfolioProjectResponse> list(Authentication authentication) {
        return portfolios.listOwnProjects(authentication);
    }
}
