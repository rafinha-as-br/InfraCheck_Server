package br.com.infracheck.server.domain;

/** Papéis de acesso definidos no briefing. O nome vira a authority {@code ROLE_<nome>} no Spring Security. */
public enum Role {
    ADMIN,
    CONTRACT_MANAGER,
    INSPECTOR;

    /** Administrador e Gestor de Contrato acessam o painel web e revisam inspeções. */
    public boolean isManagement() {
        return this == ADMIN || this == CONTRACT_MANAGER;
    }
}
