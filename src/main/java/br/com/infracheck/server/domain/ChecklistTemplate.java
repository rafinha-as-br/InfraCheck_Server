package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo de checklist (formulário configurável). O código inclui a versão (ex.: CKL-INCENDIO-V3);
 * novas versões são novos registros, nunca alterações de um modelo já usado.
 */
@Entity
@Table(name = "checklist_template")
@Getter
@Setter
@NoArgsConstructor
public class ChecklistTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemNo")
    private List<ChecklistTemplateItem> items = new ArrayList<>();
}
