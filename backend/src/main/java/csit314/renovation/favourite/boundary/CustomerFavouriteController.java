package csit314.renovation.favourite.boundary;

import csit314.renovation.favourite.control.CustomerFavouriteService;
import csit314.renovation.favourite.dto.CustomerFavouriteStatusResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/favourites")
public class CustomerFavouriteController {
    private final CustomerFavouriteService service;

    public CustomerFavouriteController(CustomerFavouriteService service) {
        this.service = service;
    }

    @GetMapping("/{projectId}")
    public CustomerFavouriteStatusResponse status(Authentication authentication,
            @PathVariable Long projectId) {
        return service.status(authentication, projectId);
    }

    @PutMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(Authentication authentication, @PathVariable Long projectId) {
        service.save(authentication, projectId);
    }

    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(Authentication authentication, @PathVariable Long projectId) {
        service.remove(authentication, projectId);
    }
}
