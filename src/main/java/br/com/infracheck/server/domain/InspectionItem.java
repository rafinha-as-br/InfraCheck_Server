package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Item de checklist dentro de uma inspeção (cópia do item do modelo no momento da criação da OS),
 * com a resposta do inspetor, as fotos e os planos de ação.
 */
@Entity
@Table(name = "inspection_item")
@Getter
@Setter
@NoArgsConstructor
public class InspectionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id")
    private Inspection inspection;

    @Column(name = "item_no", nullable = false)
    private int itemNo;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String statement;

    private String location;

    /** {@code null} enquanto o item não foi respondido. */
    @Enumerated(EnumType.STRING)
    private ItemResult result;

    /** Preenchida somente quando {@link #result} é {@link ItemResult#NON_CONFORMING}. */
    @Enumerated(EnumType.STRING)
    private Criticality criticality;

    /** Situação encontrada (não conformidade). */
    private String description;

    @Column(name = "recommended_action")
    private String recommendedAction;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @OrderBy("capturedAt")
    private List<Photo> photos = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    @OrderBy("createdAt")
    private List<ActionPlan> actionPlans = new ArrayList<>();

    public boolean isNonConforming() {
        return result == ItemResult.NON_CONFORMING;
    }
}
