package tn.driveeasy.client;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/clients")
public class ClientController {
    private final ClientService service;
    public ClientController(ClientService service) { this.service = service; }

    @GetMapping public List<Client> search(@RequestParam(required = false) String nom,
                                          @RequestParam(required = false) String cin,
                                          @RequestParam(required = false) String email) { return service.search(nom, cin, email); }
    @GetMapping("/{id}") public Client get(@PathVariable Long id) { return service.get(id); }
    @GetMapping("/{id}/eligibilite") public ClientService.Eligibility eligibility(@PathVariable Long id) { return service.eligibility(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Client create(@Valid @RequestBody ClientRequest request) { return service.create(request); }
    @PutMapping("/{id}") public Client update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }

    @PatchMapping("/{id}/categorie")
    public Client category(@PathVariable Long id, @RequestBody Map<String, String> body) { return service.updateCategory(id, body.get("categorieClient")); }

    // Route interne utilisée par location-service pour bloquer la suppression pendant une location active.
    @PatchMapping("/{id}/locations/active")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activeLocation(@PathVariable Long id, @RequestBody Map<String, Boolean> body) { service.markLocation(id, Boolean.TRUE.equals(body.get("active"))); }
}
