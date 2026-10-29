package br.com.infracheck.server.domain;

import jakarta.persistence.Embeddable;

/**
 * Coordenada GPS com precisão em metros. Dado sensível (LGPD): só é gravado nos pontos de prova
 * da inspeção (início, fim, foto) e nos eventos de auditoria.
 */
@Embeddable
public record GeoPoint(Double lat, Double lng, Double accuracy) {

    /** {@code true} quando latitude e longitude existem (precisão é opcional). */
    public boolean isComplete() {
        return lat != null && lng != null;
    }
}
