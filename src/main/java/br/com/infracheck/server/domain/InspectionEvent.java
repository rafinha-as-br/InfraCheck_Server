package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Entrada imutável da trilha de auditoria: quem fez o quê, quando e (se aplicável) onde. */
@Entity
@Table(name = "inspection_event")
@Getter
@Setter
@NoArgsConstructor
public class InspectionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    private String detail;

    @Embedded
    private GeoPoint location;
}
