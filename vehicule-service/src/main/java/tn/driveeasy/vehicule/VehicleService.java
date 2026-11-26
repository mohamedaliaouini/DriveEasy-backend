package tn.driveeasy.vehicule;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class VehicleService {
    private static final List<String> STATUSES = List.of("DISPONIBLE", "LOUE", "MAINTENANCE");
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) { this.repository = repository; }

    public List<Vehicle> search(String categorie, String carburant, String statut, java.math.BigDecimal prixMax) {
        return repository.findAll().stream()
                .filter(v -> categorie == null || v.categorie().equalsIgnoreCase(categorie))
                .filter(v -> carburant == null || v.carburant().equalsIgnoreCase(carburant))
                .filter(v -> statut == null || v.statut().equalsIgnoreCase(statut))
                .filter(v -> prixMax == null || v.prixJournalier().compareTo(prixMax) <= 0)
                .toList();
    }

    public List<Vehicle> available(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null || !dateFin.isAfter(dateDebut)) {
            throw new IllegalArgumentException("dateFin doit être postérieure à dateDebut.");
        }
        return search(null, null, "DISPONIBLE", null);
    }

    public Vehicle get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Véhicule " + id + " introuvable."));
    }

    public Vehicle create(VehicleRequest request) {
        validate(request);
        if (repository.findByRegistration(request.immatriculation()).isPresent()) {
            throw new ConflictException("Cette immatriculation existe déjà.");
        }
        return repository.saveNew(request);
    }

    public Vehicle update(Long id, VehicleRequest request) {
        Vehicle old = get(id);
        validate(request);
        repository.findByRegistration(request.immatriculation()).filter(v -> !v.id().equals(id)).ifPresent(v -> {
            throw new ConflictException("Cette immatriculation existe déjà.");
        });
        return repository.save(repository.fromRequest(old.id(), request));
    }

    public void delete(Long id) {
        Vehicle vehicle = get(id);
        if (vehicle.statut().equals("LOUE")) throw new ConflictException("Un véhicule loué ne peut pas être supprimé.");
        repository.delete(id);
    }

    public Vehicle changeStatus(Long id, String status) {
        Vehicle current = get(id);
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) throw new IllegalArgumentException("Statut invalide : " + normalized);
        return repository.save(new Vehicle(current.id(), current.immatriculation(), current.marque(), current.modele(),
                current.annee(), current.couleur(), current.carburant(), current.boite(), current.nbPlaces(),
                current.kilometrage(), current.prixJournalier(), normalized, current.imageUrl(), current.categorie()));
    }

    private void validate(VehicleRequest r) {
        if (r.annee() > LocalDate.now().getYear()) throw new IllegalArgumentException("L'année du véhicule ne peut pas dépasser l'année courante.");
        if (!List.of("ESSENCE", "DIESEL", "HYBRIDE", "ELECTRIQUE").contains(r.carburant().trim().toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Carburant invalide.");
        if (!List.of("MANUELLE", "AUTOMATIQUE").contains(r.boite().trim().toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Type de boîte invalide.");
        if (r.categorie() != null && !r.categorie().isBlank()
                && !List.of("CITADINE", "SUV", "BERLINE", "UTILITAIRE").contains(r.categorie().trim().toUpperCase(Locale.ROOT)))
            throw new IllegalArgumentException("Catégorie de véhicule invalide.");
        if (!STATUSES.contains(r.statut().trim().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Statut invalide.");
    }
}
