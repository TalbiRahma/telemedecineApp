package com.telemedecine.api.model.slot;

public enum SlotStatus {

    FREE,                 // slot disponible
    PENDING_CONFIRMATION, // réservé mais en attente de confirmation
    CONFIRMED,            // confirmé par le médecin
    CANCELLED,            // annulé (par patient ou médecin)
    COMPLETED,            // consultation terminée
    NO_SHOW,              // patient absent
    UNAVAILABLE           // indisponible (fermeture exceptionnelle, congé…)
}
