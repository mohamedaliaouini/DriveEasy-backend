package tn.driveeasy.vehicule;

import java.math.BigDecimal;

public record Vehicle(
        Long id,
        String immatriculation,
        String marque,
        String modele,
        int annee,
        String couleur,
        String carburant,
        String boite,
        int nbPlaces,
        long kilometrage,
        BigDecimal prixJournalier,
        String statut,
        String imageUrl,
        String categorie) {
}
