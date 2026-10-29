package br.com.infracheck.server.domain;

/** Resultado de um item do checklist. Todo item precisa terminar em um destes três valores. */
public enum ItemResult {
    CONFORMING,
    NON_CONFORMING,
    NOT_APPLICABLE
}
