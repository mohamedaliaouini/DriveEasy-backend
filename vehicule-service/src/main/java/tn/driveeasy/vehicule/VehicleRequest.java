package tn.driveeasy.vehicule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record VehicleRequest(
        @NotBlank String immatriculation,
        @NotBlank String marque,
        @NotBlank String modele,
        @Min(1900) int annee,
        @NotBlank String couleur,
        @NotBlank String carburant,
        @NotBlank String boite,
        @Positive int nbPlaces,
        @PositiveOrZero long kilometrage,
        @NotNull @Positive BigDecimal prixJournalier,
        @NotBlank String statut,
        String imageUrl,
        String categorie) {
}
