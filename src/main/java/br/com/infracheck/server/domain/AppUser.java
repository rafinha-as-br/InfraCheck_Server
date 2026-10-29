package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuário do sistema (administrador, gestor de contrato ou inspetor). */
@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** Hash BCrypt; a senha nunca é armazenada nem devolvida pela API. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Matrícula do inspetor, exibida na assinatura e na revisão (ex.: INS-0231). */
    private String registration;

    @Column(nullable = false)
    private boolean active = true;
}
