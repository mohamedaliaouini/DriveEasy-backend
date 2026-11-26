package tn.driveeasy.location;

import org.springframework.stereotype.Repository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class LocationRepository {
    private final ConcurrentHashMap<Long, Location> locations = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    public List<Location> findAll() { return locations.values().stream().sorted(Comparator.comparing(Location::id)).toList(); }
    public Optional<Location> findById(Long id) { return Optional.ofNullable(locations.get(id)); }
    public Location saveNew(Location location) {
        long id = sequence.incrementAndGet();
        Location saved = new Location(id, location.clientId(), location.vehiculeId(), location.dateDebut(), location.dateFinPrevue(),
                location.dateRetourEffectif(), location.kmDepart(), location.kmRetour(), location.prixJournalierApplique(),
                location.montantTotal(), location.penalite(), location.statut(), location.dateCreation());
        locations.put(id, saved);
        return saved;
    }
    public Location save(Location location) { locations.put(location.id(), location); return location; }
    public void delete(Long id) { locations.remove(id); }
}
