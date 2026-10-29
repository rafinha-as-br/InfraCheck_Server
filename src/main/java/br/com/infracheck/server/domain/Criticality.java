package br.com.infracheck.server.domain;

/** Criticidade de uma não conformidade, em ordem crescente de gravidade. */
public enum Criticality {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    /** Alta e Crítica exigem plano de ação antes da aprovação. */
    public boolean requiresActionPlan() {
        return this == HIGH || this == CRITICAL;
    }
}
