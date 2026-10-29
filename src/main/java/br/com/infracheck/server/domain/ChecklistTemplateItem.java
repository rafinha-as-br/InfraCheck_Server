package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Item de um modelo de checklist: o que verificar e onde. */
@Entity
@Table(name = "checklist_template_item")
@Getter
@Setter
@NoArgsConstructor
public class ChecklistTemplateItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id")
    private ChecklistTemplate template;

    @Column(name = "item_no", nullable = false)
    private int itemNo;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String statement;

    private String location;
}
