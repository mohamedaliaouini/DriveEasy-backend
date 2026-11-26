package tn.driveeasy.client;

import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class ClientRepository {
    private final ConcurrentHashMap<Long, Client> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> activeLocations = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public ClientRepository() {
        saveNew(new ClientRequest("12345678", "Ben Ali", "Amine", LocalDate.of(1998, 4, 12), "amine@example.com", "+21620000001", "Tunis", "P-10001", LocalDate.now().plusYears(3), "STANDARD"));
        saveNew(new ClientRequest("23456789", "Trabelsi", "Sarra", LocalDate.of(1995, 9, 5), "sarra@example.com", "+21620000002", "Sousse", "P-10002", LocalDate.now().plusYears(2), "FIDELE"));
        saveNew(new ClientRequest("34567890", "Mansour", "Youssef", LocalDate.of(1989, 2, 21), "youssef@example.com", "+21620000003", "Sfax", "P-10003", LocalDate.now().plusYears(1), "VIP"));
    }

    public List<Client> findAll() { return clients.values().stream().sorted(Comparator.comparing(Client::id)).toList(); }
    public Optional<Client> findById(Long id) { return Optional.ofNullable(clients.get(id)); }
    public Optional<Client> findByCin(String value) { return clients.values().stream().filter(c -> c.cin().equalsIgnoreCase(value)).findFirst(); }
    public Optional<Client> findByEmail(String value) { return clients.values().stream().filter(c -> c.email().equalsIgnoreCase(value)).findFirst(); }
    public Optional<Client> findByLicense(String value) { return clients.values().stream().filter(c -> c.numeroPermis().equalsIgnoreCase(value)).findFirst(); }

    public Client saveNew(ClientRequest r) {
        long id = sequence.incrementAndGet();
        Client c = toClient(id, r);
        clients.put(id, c);
        activeLocations.put(id, 0);
        return c;
    }
    public Client save(Client c) { clients.put(c.id(), c); return c; }
    public int activeCount(Long id) { return activeLocations.getOrDefault(id, 0); }
    public int updateActiveCount(Long id, boolean active) {
        return activeLocations.compute(id, (key, current) -> Math.max(0, (current == null ? 0 : current) + (active ? 1 : -1)));
    }
    public void delete(Long id) { clients.remove(id); activeLocations.remove(id); }

    private Client toClient(Long id, ClientRequest r) {
        String category = r.categorieClient() == null || r.categorieClient().isBlank() ? "STANDARD" : r.categorieClient().trim().toUpperCase();
        return new Client(id, r.cin().trim(), r.nom().trim(), r.prenom().trim(), r.dateNaissance(), r.email().trim().toLowerCase(),
                r.telephone().trim(), r.adresse().trim(), r.numeroPermis().trim(), r.dateExpirationPermis(), category,
                clients.containsKey(id) ? clients.get(id).dateInscription() : LocalDate.now());
    }
    public Client fromRequest(Long id, ClientRequest r) { return toClient(id, r); }
}
