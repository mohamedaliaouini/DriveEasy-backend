package tn.driveeasy.vehicule;

import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class VehicleRepository {
    private final ConcurrentHashMap<Long, Vehicle> vehicles = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public VehicleRepository() {
        saveNew(new VehicleRequest("123 TU 4567", "Toyota", "Yaris", 2023, "Blanc", "ESSENCE", "MANUELLE", 5, 18000, new BigDecimal("120.00"), "DISPONIBLE", "", "CITADINE"));
        saveNew(new VehicleRequest("234 TU 5678", "Hyundai", "i20", 2022, "Gris", "ESSENCE", "AUTOMATIQUE", 5, 26000, new BigDecimal("135.00"), "DISPONIBLE", "", "CITADINE"));
        saveNew(new VehicleRequest("345 TU 6789", "Dacia", "Duster", 2024, "Noir", "DIESEL", "MANUELLE", 5, 9000, new BigDecimal("180.00"), "DISPONIBLE", "", "SUV"));
        saveNew(new VehicleRequest("456 TU 7890", "Peugeot", "208", 2021, "Bleu", "ESSENCE", "MANUELLE", 5, 41000, new BigDecimal("110.00"), "MAINTENANCE", "", "CITADINE"));
    }

    public List<Vehicle> findAll() {
        return vehicles.values().stream().sorted(Comparator.comparing(Vehicle::id)).toList();
    }

    public Optional<Vehicle> findById(Long id) { return Optional.ofNullable(vehicles.get(id)); }

    public Optional<Vehicle> findByRegistration(String registration) {
        return vehicles.values().stream().filter(v -> v.immatriculation().equalsIgnoreCase(registration)).findFirst();
    }

    public Vehicle saveNew(VehicleRequest request) {
        long id = sequence.incrementAndGet();
        Vehicle vehicle = toVehicle(id, request);
        vehicles.put(id, vehicle);
        return vehicle;
    }

    public Vehicle save(Vehicle vehicle) { vehicles.put(vehicle.id(), vehicle); return vehicle; }

    private Vehicle toVehicle(Long id, VehicleRequest r) {
        return new Vehicle(id, r.immatriculation().trim(), r.marque().trim(), r.modele().trim(), r.annee(),
                r.couleur().trim(), r.carburant().trim().toUpperCase(), r.boite().trim().toUpperCase(),
                r.nbPlaces(), r.kilometrage(), r.prixJournalier(), r.statut().trim().toUpperCase(), r.imageUrl(),
                r.categorie() == null || r.categorie().isBlank() ? "CITADINE" : r.categorie().trim().toUpperCase());
    }

    public Vehicle fromRequest(Long id, VehicleRequest r) { return toVehicle(id, r); }
    public void delete(Long id) { vehicles.remove(id); }
}
