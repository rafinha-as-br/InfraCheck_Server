package br.com.infracheck.server.web.dto;

import br.com.infracheck.server.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contratos JSON da API (requisições e respostas). São registros imutáveis, separados das entidades JPA
 * para que o modelo de persistência possa evoluir sem quebrar app e painel.
 *
 * <p>Convenções: chaves em camelCase; enums como constantes em inglês (ex.: {@code IN_PROGRESS}), cujo
 * rótulo em português é responsabilidade de cada cliente; instantes em ISO-8601 UTC.
 */
public final class Dtos {

    private Dtos() {}

    // ---------------------------------------------------------------- autenticação

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record LoginResponse(String token, Instant expiresAt, UserDto user) {}

    public record UserDto(UUID id, String name, String email, Role role, String registration) {}

    // ---------------------------------------------------------------- cadastros de apoio

    public record ContractDto(UUID id, String code, String clientName, String segment, String responsibleName, String address) {}

    public record ChecklistDto(UUID id, String code, String name, int itemCount) {}

    /** Inspetor disponível para atribuição, com a carga de OS previstas para hoje. */
    public record InspectorDto(UUID id, String name, String registration, long todayCount) {}

    // ---------------------------------------------------------------- requisições de inspeção

    public record CreateInspectionRequest(
            @NotNull UUID contractId,
            @NotNull UUID checklistId,
            @NotNull UUID inspectorId,
            @NotNull InspectionType type,
            @NotNull LocalDate scheduledDate,
            @NotNull Shift shift,
            @Size(max = 1000) String notes) {}

    /** Início da execução em campo (pode ser reenviado: a operação é idempotente). */
    public record StartRequest(@NotNull Instant startedAt, GeoPoint location) {}

    public record ItemAnswer(
            @NotNull UUID itemId,
            @NotNull ItemResult result,
            Criticality criticality,
            @Size(max = 2000) String description,
            @Size(max = 1000) String recommendedAction) {}

    /**
     * Pacote completo da execução, enviado de uma vez ao encerrar (ou ao sincronizar depois de um período
     * offline). {@code signature} é um PNG em base64, com ou sem o prefixo {@code data:image/png;base64,}.
     */
    public record SubmitRequest(
            @NotNull Instant startedAt,
            GeoPoint startLocation,
            @NotNull Instant finishedAt,
            GeoPoint endLocation,
            @Size(max = 2000) String generalNotes,
            @NotBlank String signature,
            @NotEmpty @Valid List<ItemAnswer> items) {}

    public record ActionPlanRequest(
            @NotNull UUID itemId,
            @NotBlank @Size(max = 120) String responsible,
            @NotNull LocalDate dueDate,
            @NotBlank @Size(max = 1000) String action) {}

    public record RejectRequest(@NotBlank @Size(max = 1000) String reason) {}

    // ---------------------------------------------------------------- respostas de inspeção

    public record PersonDto(UUID id, String name, String registration) {}

    public record PhotoDto(UUID id, Instant capturedAt, Double lat, Double lng, Double accuracy) {}

    public record ActionPlanDto(UUID id, String responsible, LocalDate dueDate, String action, String createdBy, Instant createdAt) {}

    public record ItemDto(
            UUID id,
            int itemNo,
            String category,
            String statement,
            String location,
            ItemResult result,
            Criticality criticality,
            String description,
            String recommendedAction,
            List<PhotoDto> photos,
            List<ActionPlanDto> actionPlans) {}

    public record EventDto(UUID id, EventType type, String actor, Instant occurredAt, String detail, GeoPoint location) {}

    /**
     * Totais calculados no servidor. {@code conformityPercent} = conformes / total de itens (0 a 100),
     * como no design (38 de 42 = 90,5%).
     */
    public record Stats(
            int total,
            int answered,
            int conforming,
            int nonConforming,
            int notApplicable,
            int photos,
            Criticality maxCriticality,
            double conformityPercent) {}

    /** Linha das listagens (painel e app). */
    public record InspectionSummary(
            UUID id,
            long number,
            InspectionStatus status,
            InspectionType type,
            boolean urgent,
            LocalDate scheduledDate,
            Shift shift,
            String clientName,
            String address,
            String checklistCode,
            PersonDto inspector,
            String managerName,
            Instant startedAt,
            Instant finishedAt,
            String reviewComment,
            String notes,
            Stats stats) {}

    /** Visão completa de uma inspeção: execução, evidências, planos de ação e auditoria. */
    public record InspectionDetail(
            UUID id,
            long number,
            InspectionStatus status,
            InspectionType type,
            boolean urgent,
            LocalDate scheduledDate,
            Shift shift,
            String notes,
            ContractDto contract,
            String checklistCode,
            String checklistName,
            PersonDto inspector,
            PersonDto manager,
            Instant createdAt,
            Instant startedAt,
            GeoPoint startLocation,
            Instant finishedAt,
            GeoPoint endLocation,
            String generalNotes,
            boolean hasSignature,
            String reviewComment,
            Instant reviewedAt,
            String reviewedBy,
            Stats stats,
            List<ItemDto> items,
            List<EventDto> events) {}

    public record PageDto<T>(List<T> content, long total, int page, int size) {}

    // ---------------------------------------------------------------- painel

    public record InspectorLoad(UUID inspectorId, String name, long count) {}

    public record ReviewQueueEntry(InspectionSummary inspection, long waitingMinutes) {}

    /**
     * Indicadores do dashboard. {@code avgHoursToApproval} é a média entre o encerramento e a aprovação
     * (substitui "tempo até o relatório" do design enquanto o PDF não existe).
     */
    public record Dashboard(
            long inProgress,
            long startedToday,
            long awaitingReview,
            long awaitingReviewOver12h,
            long overdue,
            Double avgHoursToApproval,
            Map<InspectionStatus, Long> byStatus,
            List<ReviewQueueEntry> reviewQueue,
            List<InspectorLoad> inspectorLoadToday) {}
}
