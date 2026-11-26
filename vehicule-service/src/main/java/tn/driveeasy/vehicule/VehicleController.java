package tn.driveeasy.vehicule;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/vehicules")
public class VehicleController {
    private final VehicleService service;
    public VehicleController(VehicleService service) { this.service = service; }

    @GetMapping
    public List<Vehicle> search(@RequestParam(required = false) String categorie,
                                @RequestParam(required = false) String carburant,
                                @RequestParam(required = false) String statut,
                                @RequestParam(required = false) BigDecimal prixMax) {
        return service.search(categorie, carburant, statut, prixMax);
    }

    @GetMapping("/disponibles")
    public List<Vehicle> available(@RequestParam LocalDate dateDebut, @RequestParam LocalDate dateFin) {
        return service.available(dateDebut, dateFin);
    }

    @GetMapping("/{id}") public Vehicle get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Vehicle create(@Valid @RequestBody VehicleRequest request) { return service.create(request); }
    @PutMapping("/{id}") public Vehicle update(@PathVariable Long id, @Valid @RequestBody VehicleRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }

    @PatchMapping("/{id}/statut")
    public Vehicle status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.changeStatus(id, body.get("statut"));
    }
}
