package tn.driveeasy.location;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Location(Long id, Long clientId, Long vehiculeId, LocalDate dateDebut, LocalDate dateFinPrevue,
                       LocalDate dateRetourEffectif, Long kmDepart, Long kmRetour,
                       BigDecimal prixJournalierApplique, BigDecimal montantTotal, BigDecimal penalite,
                       String statut, LocalDateTime dateCreation) { }
