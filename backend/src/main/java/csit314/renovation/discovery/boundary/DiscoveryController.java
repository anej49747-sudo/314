package csit314.renovation.discovery.boundary;

import csit314.renovation.discovery.control.DiscoveryService;
import csit314.renovation.project.control.PublicProjectDetailsService;
import csit314.renovation.discovery.control.DiscoveryService.CatalogueInput;
import csit314.renovation.discovery.control.DiscoveryService.PreferencesInput;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryController {
    private final DiscoveryService service;
    private final PublicProjectDetailsService publicProjects;
    public DiscoveryController(DiscoveryService service,PublicProjectDetailsService publicProjects){this.service=service;this.publicProjects=publicProjects;}
    @GetMapping("/catalogues/{kind}")
    public List<Map<String,Object>> catalogue(@PathVariable String kind){return service.catalogue(kind);}
    @PostMapping("/catalogues/{kind}") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> create(@PathVariable String kind,@RequestBody CatalogueInput value, Authentication auth){return service.saveCatalogue(kind,null,value,auth);}
    @PutMapping("/catalogues/{kind}/{id}")
    public Map<String,Object> change(@PathVariable String kind,@PathVariable Long id,@RequestBody CatalogueInput value,Authentication auth){return service.saveCatalogue(kind,id,value,auth);}
    @DeleteMapping("/catalogues/{kind}/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String kind,@PathVariable Long id,Authentication auth){service.deleteCatalogue(kind,id,auth);}
    @GetMapping("/projects/{id}/metadata")
    public Map<String,Object> metadata(@PathVariable Long id){publicProjects.requirePublicProject(id);return service.metadata(id);}
    @GetMapping("/designer/projects/{id}/metadata")
    public Map<String,Object> ownMetadata(@PathVariable Long id,Authentication auth){
        return service.ownMetadata(id,auth);
    }
    @GetMapping("/customer/preferences")
    public Map<String,Object> preferences(Authentication auth){return service.preferences(auth);}
    @PutMapping("/customer/preferences")
    public Map<String,Object> savePreferences(Authentication auth,@RequestBody PreferencesInput input){return service.savePreferences(auth,input);}
    @PostMapping("/customer/preferences/skip") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void skip(Authentication auth){service.skip(auth);}
    @GetMapping("/explore")
    public Map<String,Object> explore(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="6") int size,
            @RequestParam(defaultValue="") List<Long> excludeIds, Authentication auth) {
        return service.explore(auth,page,size,excludeIds==null?java.util.Set.of():new java.util.HashSet<>(excludeIds));
    }
    @GetMapping("/projects")
    public Map<String,Object> projects(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="8") int size,@RequestParam(defaultValue="false") boolean reel,@RequestParam(defaultValue="") List<Long> excludeIds,Authentication auth){return service.recommendations(auth,page,size,reel,excludeIds==null?java.util.Set.of():new java.util.HashSet<>(excludeIds));}
    @GetMapping("/designers")
    public List<Map<String,Object>> designers(@RequestParam(defaultValue="6") int limit,Authentication auth){return service.designerRecommendations(auth,limit);}
}
