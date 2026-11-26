package tn.driveeasy.location;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LocationService {
    private static final String RESERVED = "RESERVEE";
    private static final String ONGOING = "EN_COURS";
    private static final String COMPLETE = "TERMINEE";
    private static final String CANCELLED = "ANNULEE";

    private final LocationRepository repository;
    private final WebClient client;
    @Value("${driveeasy.location.late-penalty-factor:1.5}") private BigDecimal latePenaltyFactor;
    @Value("${driveeasy.location.loyal-discount:0.05}") private BigDecimal loyalDiscount;
    @Value("${driveeasy.location.vip-discount:0.10}") private BigDecimal vipDiscount;

    public LocationService(LocationRepository repository, WebClient.Builder builder) {
        this.repository = repository;
        this.client = builder.build();
    }

    public PageResult list(int page, int size, String status, Long clientId, Long vehicleId,
                           LocalDate dateFrom, LocalDate dateTo) {
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Pagination invalide (page >= 0, size entre 1 et 100).");
        List<Location> filtered = repository.findAll().stream()
                .filter(l -> status == null || l.statut().equalsIgnoreCase(status))
                .filter(l -> clientId == null || l.clientId().equals(clientId))
                .filter(l -> vehicleId == null || l.vehiculeId().equals(vehicleId))
                .filter(l -> dateFrom == null || !l.dateFinPrevue().isBefore(dateFrom))
                .filter(l -> dateTo == null || !l.dateDebut().isAfter(dateTo))
                .toList();
        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        return new PageResult(filtered.subList(start, end), page, size, filtered.size(), (int) Math.ceil(filtered.size() / (double) size));
    }

    public LocationView get(Long id) {
        Location l = find(id);
        ClientInfo c = call(() -> client.get().uri("http://client-service/clients/{id}", l.clientId())
                .retrieve().bodyToMono(ClientInfo.class).block());
        VehicleInfo v = getVehicle(l.vehiculeId());
        return new LocationView(l, c, v);
    }

    public PriceSimulation simulate(Long clientId, Long vehicleId, LocalDate start, LocalDate end) {
        validatePeriod(start, end);
        ClientInfo c = getClient(clientId);
        VehicleInfo v = getVehicle(vehicleId);
        long days = ChronoUnit.DAYS.between(start, end);
        BigDecimal discount = discountFor(c.categorieClient());
        BigDecimal gross = v.prixJournalier().multiply(BigDecimal.valueOf(days));
        BigDecimal total = gross.multiply(BigDecimal.ONE.subtract(discount)).setScale(2, RoundingMode.HALF_UP);
        return new PriceSimulation(days, v.prixJournalier(), c.categorieClient(), discount, total);
    }

    public synchronized Location create(LocationRequest request) {
        validatePeriod(request.dateDebut(), request.dateFinPrevue());
        if (request.dateDebut().isBefore(LocalDate.now())) throw new IllegalArgumentException("La date de début ne peut pas être passée.");
        ClientEligibility eligibility = call(() -> client.get().uri("http://client-service/clients/{id}/eligibilite", request.clientId())
                .retrieve().bodyToMono(ClientEligibility.class).block());
        if (!eligibility.eligible()) throw new IllegalArgumentException(eligibility.message());

        List<VehicleInfo> available = call(() -> client.get().uri(uri -> uri
                        .scheme("http").host("vehicule-service").path("/vehicules/disponibles")
                        .queryParam("dateDebut", request.dateDebut()).queryParam("dateFin", request.dateFinPrevue()).build())
                .retrieve().bodyToFlux(VehicleInfo.class).collectList().block());
        VehicleInfo vehicle = available.stream().filter(v -> v.id().equals(request.vehiculeId())).findFirst()
                .orElseThrow(() -> new ConflictException("Le véhicule n'est pas disponible pour cette période."));

        boolean overlaps = repository.findAll().stream().filter(l -> l.vehiculeId().equals(request.vehiculeId()))
                .filter(l -> !l.statut().equals(CANCELLED))
                .anyMatch(l -> request.dateDebut().isBefore(l.dateFinPrevue()) && l.dateDebut().isBefore(request.dateFinPrevue()));
        if (overlaps) throw new ConflictException("Le véhicule est déjà réservé sur une partie de cette période.");

        PriceSimulation price = simulate(request.clientId(), request.vehiculeId(), request.dateDebut(), request.dateFinPrevue());
        Location toSave = new Location(null, request.clientId(), request.vehiculeId(), request.dateDebut(), request.dateFinPrevue(),
                null, null, null, price.prixJournalier(), price.montantEstime(), BigDecimal.ZERO.setScale(2), RESERVED, LocalDateTime.now());
        Location saved = repository.saveNew(toSave);
        try {
            markClientActive(request.clientId(), true);
        } catch (RuntimeException ex) {
            repository.delete(saved.id());
            throw ex;
        }
        return saved;
    }

    public Location start(Long id, Long kmDepart) {
        if (kmDepart == null || kmDepart < 0) throw new IllegalArgumentException("Le kilométrage de départ doit être positif ou nul.");
        Location l = find(id);
        if (!l.statut().equals(RESERVED)) throw new ConflictException("Seule une réservation peut être démarrée.");
        if (LocalDate.now().isBefore(l.dateDebut())) throw new ConflictException("La location ne peut pas démarrer avant la date prévue.");
        patchVehicleStatus(l.vehiculeId(), "LOUE");
        return repository.save(copy(l, null, kmDepart, null, l.montantTotal(), l.penalite(), ONGOING));
    }

    public Location finish(Long id, Long kmRetour) {
        Location l = find(id);
        if (!l.statut().equals(ONGOING)) throw new ConflictException("Seule une location en cours peut être clôturée.");
        if (kmRetour == null || kmRetour < l.kmDepart()) throw new IllegalArgumentException("Le kilométrage de retour doit être supérieur ou égal au kilométrage de départ.");
        LocalDate returnedOn = LocalDate.now();
        long lateDays = Math.max(0, ChronoUnit.DAYS.between(l.dateFinPrevue(), returnedOn));
        BigDecimal penalty = l.prixJournalierApplique().multiply(latePenaltyFactor).multiply(BigDecimal.valueOf(lateDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = l.montantTotal().add(penalty).setScale(2, RoundingMode.HALF_UP);

        patchVehicleStatus(l.vehiculeId(), "DISPONIBLE");
        try {
            markClientActive(l.clientId(), false);
        } catch (RuntimeException ex) {
            try { patchVehicleStatus(l.vehiculeId(), "LOUE"); } catch (RuntimeException ignored) { }
            throw ex;
        }
        Location completed = copy(l, returnedOn, l.kmDepart(), kmRetour, total, penalty, COMPLETE);
        try {
            updateLoyaltyCategory(l.clientId());
        } catch (RuntimeException ex) {
            try { markClientActive(l.clientId(), true); } catch (RuntimeException ignored) { }
            try { patchVehicleStatus(l.vehiculeId(), "LOUE"); } catch (RuntimeException ignored) { }
            throw ex;
        }
        return repository.save(completed);
    }

    public Location cancel(Long id) {
        Location l = find(id);
        if (!l.statut().equals(RESERVED)) throw new ConflictException("Seule une réservation peut être annulée.");
        markClientActive(l.clientId(), false);
        return repository.save(copy(l, null, l.kmDepart(), l.kmRetour(), l.montantTotal(), l.penalite(), CANCELLED));
    }

    public Statistics statistics() {
        List<Location> all = repository.findAll();
        List<VehicleInfo> vehicles = call(() -> client.get().uri("http://vehicule-service/vehicules")
                .retrieve().bodyToFlux(VehicleInfo.class).collectList().block());
        LocalDate now = LocalDate.now();
        BigDecimal revenue = all.stream().filter(l -> l.statut().equals(COMPLETE)).map(Location::montantTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthly = all.stream().filter(l -> l.statut().equals(COMPLETE) && YearMonth.from(l.dateRetourEffectif()).equals(YearMonth.from(now)))
                .map(Location::montantTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        long occupied = all.stream().filter(l -> l.statut().equals(ONGOING)
                || (l.statut().equals(RESERVED) && !now.isBefore(l.dateDebut()) && now.isBefore(l.dateFinPrevue()))).map(Location::vehiculeId).distinct().count();
        BigDecimal occupancy = vehicles.isEmpty() ? BigDecimal.ZERO : BigDecimal.valueOf(occupied * 100.0 / vehicles.size()).setScale(2, RoundingMode.HALF_UP);
        List<TopVehicle> top = all.stream().filter(l -> !l.statut().equals(CANCELLED))
                .collect(java.util.stream.Collectors.groupingBy(Location::vehiculeId, java.util.stream.Collectors.counting()))
                .entrySet().stream().sorted(Map.Entry.<Long, Long>comparingByValue().reversed()).limit(5)
                .map(e -> new TopVehicle(e.getKey(), e.getValue())).toList();
        return new Statistics(revenue, monthly, occupancy, all.stream().filter(l -> l.statut().equals(ONGOING)).count(), top);
    }

    private BigDecimal discountFor(String category) {
        if ("VIP".equalsIgnoreCase(category)) return vipDiscount;
        if ("FIDELE".equalsIgnoreCase(category)) return loyalDiscount;
        return BigDecimal.ZERO;
    }
    private void updateLoyaltyCategory(Long clientId) {
        long count = repository.findAll().stream().filter(l -> l.clientId().equals(clientId) && l.statut().equals(COMPLETE)).count() + 1;
        String category = count >= 15 ? "VIP" : count >= 5 ? "FIDELE" : "STANDARD";
        call(() -> client.patch().uri("http://client-service/clients/{id}/categorie", clientId)
                .bodyValue(Map.of("categorieClient", category)).retrieve().bodyToMono(ClientInfo.class).block());
    }
    private void markClientActive(Long id, boolean active) {
        call(() -> client.patch().uri("http://client-service/clients/{id}/locations/active", id)
                .bodyValue(Map.of("active", active)).retrieve().toBodilessEntity().block());
    }
    private void patchVehicleStatus(Long id, String status) {
        call(() -> client.patch().uri("http://vehicule-service/vehicules/{id}/statut", id)
                .bodyValue(Map.of("statut", status)).retrieve().bodyToMono(VehicleInfo.class).block());
    }
    private VehicleInfo getVehicle(Long id) {
        return call(() -> client.get().uri("http://vehicule-service/vehicules/{id}", id)
                .retrieve().bodyToMono(VehicleInfo.class).block());
    }
    private ClientInfo getClient(Long id) {
        return call(() -> client.get().uri("http://client-service/clients/{id}", id)
                .retrieve().bodyToMono(ClientInfo.class).block());
    }
    private Location find(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Location " + id + " introuvable.")); }
    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null || end == null || !end.isAfter(start)) throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
    }
    private Location copy(Location l, LocalDate returned, Long kmStart, Long kmEnd, BigDecimal total, BigDecimal penalty, String status) {
        return new Location(l.id(), l.clientId(), l.vehiculeId(), l.dateDebut(), l.dateFinPrevue(), returned,
                kmStart, kmEnd, l.prixJournalierApplique(), total, penalty, status, l.dateCreation());
    }
    private <T> T call(java.util.function.Supplier<T> call) {
        try { return Objects.requireNonNull(call.get(), "Le microservice distant n'a retourné aucune donnée."); }
        catch (RuntimeException ex) {
            if (ex instanceof RemoteServiceException) throw ex;
            throw new RemoteServiceException("Un service métier est indisponible ou a refusé la requête : " + ex.getMessage(), ex);
        }
    }

    public record ClientInfo(Long id, String cin, String nom, String prenom, String email, String categorieClient) { }
    public record ClientEligibility(Long clientId, int age, boolean permisValide, boolean eligible, String message) { }
    public record VehicleInfo(Long id, String immatriculation, String marque, String modele, BigDecimal prixJournalier, String statut) { }
    public record LocationView(Location location, ClientInfo client, VehicleInfo vehicule) { }
    public record PriceSimulation(long nombreJours, BigDecimal prixJournalier, String categorieClient, BigDecimal remise, BigDecimal montantEstime) { }
    public record PageResult(List<Location> items, int page, int size, int totalElements, int totalPages) { }
    public record TopVehicle(Long vehiculeId, long nombreLocations) { }
    public record Statistics(BigDecimal chiffreAffairesTotal, BigDecimal chiffreAffairesMois, BigDecimal tauxOccupation, long locationsEnCours, List<TopVehicle> topVehicules) { }
}

class NotFoundException extends RuntimeException { NotFoundException(String message) { super(message); } }
class ConflictException extends RuntimeException { ConflictException(String message) { super(message); } }
class RemoteServiceException extends RuntimeException {
    RemoteServiceException(String message, Throwable cause) { super(message, cause); }
}
