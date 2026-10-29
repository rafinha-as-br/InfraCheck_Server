package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Contrato com o cliente e o local onde as inspeções acontecem.
 *
 * <p>Simplificação do MVP: cliente, contrato e local compartilham um único cadastro. Quando o modelo
 * de clientes/locais for definido, esta entidade deve ser dividida.
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Código do contrato (ex.: CT-2024-117). */
    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    private String segment;

    /** Responsável no local (ex.: síndica). */
    @Column(name = "responsible_name")
    private String responsibleName;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private boolean active = true;
}
