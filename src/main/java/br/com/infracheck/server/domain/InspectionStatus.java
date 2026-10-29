package br.com.infracheck.server.domain;

/**
 * Ciclo de vida da inspeção (OS): {@code PENDING → IN_PROGRESS → COMPLETED → APPROVED | REJECTED}.
 * {@code REJECTED} volta ao inspetor, que corrige e reenvia ({@code COMPLETED} de novo).
 */
public enum InspectionStatus {
    /** Pendente: criada e atribuída, ainda não iniciada em campo. */
    PENDING,
    /** Em andamento: o inspetor iniciou a vistoria. */
    IN_PROGRESS,
    /** Concluída: encerrada e assinada pelo inspetor, aguardando revisão do gestor. */
    COMPLETED,
    APPROVED,
    REJECTED;

    /** Ainda precisa de ação do inspetor em campo (inclui correção após rejeição). */
    public boolean isEditableByInspector() {
        return this == PENDING || this == IN_PROGRESS || this == REJECTED;
    }
}
