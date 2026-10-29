package br.com.infracheck.server.domain;

/** Tipos de evento da trilha de auditoria da inspeção. */
public enum EventType {
    CREATED,
    STARTED,
    SUBMITTED,
    /** Dados coletados offline chegaram ao servidor bem depois do encerramento. */
    SYNCED,
    ACTION_PLAN_SAVED,
    APPROVED,
    REJECTED
}
