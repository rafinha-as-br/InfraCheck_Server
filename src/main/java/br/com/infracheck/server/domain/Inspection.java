package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Inspeção / ordem de serviço (OS): agrega o contrato, o checklist copiado, o inspetor responsável,
 * as evidências de execução (horários, GPS, assinatura) e o histórico de auditoria.
 */
@Entity
@Table(name = "inspection")
@Getter
@Setter
@NoArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Número da OS exibido ao usuário (ex.: 1047). */
    @Column(name = "os_number", nullable = false, unique = true)
    private long number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    @Column(name = "checklist_code", nullable = false)
    private String checklistCode;

    @Column(name = "checklist_name", nullable = false)
    private String checklistName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspector_id")
    private AppUser inspector;

    /** Gestor que criou e atribuiu a OS. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private AppUser manager;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionType type;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;

    /** Observações prévias do gestor, mostradas ao inspetor. */
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionStatus status = InspectionStatus.PENDING;

    @Column(name = "started_at")
    private Instant startedAt;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "lat", column = @Column(name = "start_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "start_lng")),
        @AttributeOverride(name = "accuracy", column = @Column(name = "start_accuracy"))
    })
    private GeoPoint startLocation;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "lat", column = @Column(name = "end_lat")),
        @AttributeOverride(name = "lng", column = @Column(name = "end_lng")),
        @AttributeOverride(name = "accuracy", column = @Column(name = "end_accuracy"))
    })
    private GeoPoint endLocation;

    @Column(name = "general_notes")
    private String generalNotes;

    /** Caminho relativo da assinatura (PNG) no armazenamento. */
    @Column(name = "signature_path")
    private String signaturePath;

    /** Motivo informado pelo gestor na rejeição; o inspetor lê no app. */
    @Column(name = "review_comment")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private AppUser reviewedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemNo")
    private List<InspectionItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "inspection", cascade = CascadeType.ALL)
    @OrderBy("occurredAt")
    private List<InspectionEvent> events = new ArrayList<>();

    /** Acrescenta uma entrada à trilha de auditoria. */
    public InspectionEvent addEvent(EventType type, AppUser actor, Instant at, String detail, GeoPoint location) {
        InspectionEvent e = new InspectionEvent();
        e.setInspection(this);
        e.setType(type);
        e.setActor(actor);
        e.setOccurredAt(at);
        e.setDetail(detail);
        e.setLocation(location);
        events.add(e);
        return e;
    }
}
