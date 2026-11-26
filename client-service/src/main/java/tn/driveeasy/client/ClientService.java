package tn.driveeasy.client;

import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;

@Service
public class ClientService {
    private static final List<String> CATEGORIES = List.of("STANDARD", "FIDELE", "VIP");
    private final ClientRepository repository;
    public ClientService(ClientRepository repository) { this.repository = repository; }

    public List<Client> search(String nom, String cin, String email) {
        if ((nom == null || nom.isBlank()) && (cin == null || cin.isBlank()) && (email == null || email.isBlank())) return repository.findAll();
        String nameTerm = nom == null ? "" : nom.trim().toLowerCase(Locale.ROOT);
        String cinTerm = cin == null ? "" : cin.trim().toLowerCase(Locale.ROOT);
        String emailTerm = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return repository.findAll().stream().filter(c ->
                (nameTerm.isEmpty() || c.nom().toLowerCase(Locale.ROOT).contains(nameTerm) || c.prenom().toLowerCase(Locale.ROOT).contains(nameTerm))
                && (cinTerm.isEmpty() || c.cin().toLowerCase(Locale.ROOT).contains(cinTerm))
                && (emailTerm.isEmpty() || c.email().toLowerCase(Locale.ROOT).contains(emailTerm))).toList();
    }

    public Client get(Long id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Client " + id + " introuvable.")); }

    public Eligibility eligibility(Long id) {
        Client c = get(id);
        int age = Period.between(c.dateNaissance(), LocalDate.now()).getYears();
        boolean permitValid = !c.dateExpirationPermis().isBefore(LocalDate.now());
        return new Eligibility(id, age, permitValid, age >= 21 && permitValid,
                age < 21 ? "Le client doit avoir au moins 21 ans." : !permitValid ? "Le permis de conduire est expiré." : "Client éligible.");
    }

    public Client create(ClientRequest request) {
        validate(request);
        ensureUnique(request, null);
        return repository.saveNew(request);
    }

    public Client update(Long id, ClientRequest request) {
        Client old = get(id);
        validate(request);
        ensureUnique(request, id);
        return repository.save(repository.fromRequest(old.id(), request));
    }

    public void delete(Long id) {
        get(id);
        if (repository.activeCount(id) > 0) throw new ConflictException("Un client ayant une location active ne peut pas être supprimé.");
        repository.delete(id);
    }

    public Client updateCategory(Long id, String category) {
        Client c = get(id);
        String normalized = category == null ? "" : category.trim().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) throw new IllegalArgumentException("Catégorie client invalide.");
        return repository.save(new Client(c.id(), c.cin(), c.nom(), c.prenom(), c.dateNaissance(), c.email(), c.telephone(),
                c.adresse(), c.numeroPermis(), c.dateExpirationPermis(), normalized, c.dateInscription()));
    }

    public void markLocation(Long id, boolean active) {
        get(id);
        repository.updateActiveCount(id, active);
    }

    private void validate(ClientRequest r) {
        if (r.dateNaissance().isAfter(LocalDate.now())) throw new IllegalArgumentException("La date de naissance ne peut pas être future.");
        if (r.dateExpirationPermis().isBefore(r.dateNaissance())) throw new IllegalArgumentException("La date d'expiration du permis est invalide.");
        if (r.categorieClient() != null && !r.categorieClient().isBlank()
                && !CATEGORIES.contains(r.categorieClient().trim().toUpperCase(Locale.ROOT))) throw new IllegalArgumentException("Catégorie client invalide.");
    }

    private void ensureUnique(ClientRequest r, Long exceptId) {
        repository.findByCin(r.cin()).filter(c -> !c.id().equals(exceptId)).ifPresent(c -> { throw new ConflictException("Ce CIN existe déjà."); });
        repository.findByEmail(r.email()).filter(c -> !c.id().equals(exceptId)).ifPresent(c -> { throw new ConflictException("Cet email existe déjà."); });
        repository.findByLicense(r.numeroPermis()).filter(c -> !c.id().equals(exceptId)).ifPresent(c -> { throw new ConflictException("Ce numéro de permis existe déjà."); });
    }

    public record Eligibility(Long clientId, int age, boolean permisValide, boolean eligible, String message) { }
}
