package tn.driveeasy.location;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/locations")
public class LocationController {
    private final LocationService service;
    public LocationController(LocationService service) { this.service = service; }

    @GetMapping
    public LocationService.PageResult list(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String statut,
                                           @RequestParam(required = false) Long clientId,
                                           @RequestParam(required = false) Long vehiculeId,
                                           @RequestParam(required = false) LocalDate dateDebut,
                                           @RequestParam(required = false) LocalDate dateFin) {
        return service.list(page, size, statut, clientId, vehiculeId, dateDebut, dateFin);
    }

    @GetMapping("/statistiques") public LocationService.Statistics statistics() { return service.statistics(); }
    @GetMapping("/{id}") public LocationService.LocationView get(@PathVariable Long id) { return service.get(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Location create(@Valid @RequestBody LocationRequest request) { return service.create(request); }

    @PostMapping("/simulation")
    public LocationService.PriceSimulation simulate(@Valid @RequestBody PriceSimulationRequest request) {
        return service.simulate(request.clientId(), request.vehiculeId(), request.dateDebut(), request.dateFinPrevue());
    }

    @PatchMapping("/{id}/depart")
    public Location start(@PathVariable Long id, @RequestBody Map<String, Long> body) { return service.start(id, body.get("kmDepart")); }

    @PatchMapping("/{id}/retour")
    public Location finish(@PathVariable Long id, @RequestBody Map<String, Long> body) { return service.finish(id, body.get("kmRetour")); }

    @PatchMapping("/{id}/annulation") public Location cancel(@PathVariable Long id) { return service.cancel(id); }

    public record PriceSimulationRequest(@NotNull Long clientId, @NotNull Long vehiculeId,
                                         @NotNull LocalDate dateDebut, @NotNull LocalDate dateFinPrevue) { }
}
