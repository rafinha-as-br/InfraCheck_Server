package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plano de ação registrado pelo gestor para corrigir uma não conformidade. */
@Entity
@Table(name = "action_plan")
@Getter
@Setter
@NoArgsConstructor
public class ActionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private InspectionItem item;

    @Column(nullable = false)
    private String responsible;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "action_text", nullable = false)
    private String action;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
