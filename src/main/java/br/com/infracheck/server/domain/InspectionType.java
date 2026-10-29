package br.com.infracheck.server.domain;

/** Tipos de inspeção listados no formulário "Nova inspeção" do design. */
public enum InspectionType {
    PREVENTIVE,
    CORRECTIVE,
    PRE_DELIVERY,
    AUDIT,
    EMERGENCY;

    /** Emergenciais aparecem com o selo "Urgente". */
    public boolean isUrgent() {
        return this == EMERGENCY;
    }
}
