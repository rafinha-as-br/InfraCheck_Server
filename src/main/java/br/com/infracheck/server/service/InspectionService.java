package br.com.infracheck.server.service;

import br.com.infracheck.server.domain.*;
import br.com.infracheck.server.repository.*;
import br.com.infracheck.server.web.ApiException;
import br.com.infracheck.server.web.dto.Dtos.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Casos de uso das inspeções: programação pelo gestor, execução/envio pelo inspetor e revisão.
 *
 * <p>Mudanças de estado válidas: {@code PENDING → IN_PROGRESS → COMPLETED → APPROVED | REJECTED} e
 * {@code REJECTED → COMPLETED} (reenvio após correção). Toda mudança relevante vira um {@link InspectionEvent}.
 *
 * <p>Regras aplicadas no servidor (briefing/Confluence): todo item respondido; não conformidade com
 * criticidade e ao menos uma foto; início/fim com horário e GPS; assinatura; plano de ação para
 * criticidade Alta/Crítica antes da aprovação. Regras adicionais de negócio ainda não foram definidas.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InspectionService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    private final InspectionRepository inspections;
    private final ContractRepository contracts;
    private final ChecklistTemplateRepository templates;
    private final UserRepository users;
    private final PhotoRepository photos;
    private final PhotoStorage storage;
    private final Clock clock;
    private final EntityManager em;

    // ------------------------------------------------------------------ consulta

    /**
     * Lista paginada. Inspetores só enxergam as próprias OS, independentemente do filtro informado.
     *
     * @param q busca livre: número da OS, cliente ou nome do inspetor
     */
    @Transactional(readOnly = true)
    public PageDto<InspectionSummary> list(
            AppUser user, InspectionStatus status, UUID inspectorId, InspectionType type, String q, Pageable pageable) {
        Page<Inspection> page = inspections.findAll(filter(user, status, inspectorId, type, q), pageable);
        return new PageDto<>(
                page.getContent().stream().map(InspectionMapper::summary).toList(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize());
    }

    /** Contagem por status no mesmo escopo de visibilidade da listagem (alimenta os chips do painel). */
    @Transactional(readOnly = true)
    public Map<InspectionStatus, Long> countByStatus(AppUser user) {
        Map<InspectionStatus, Long> counts = new EnumMap<>(InspectionStatus.class);
        for (InspectionStatus s : InspectionStatus.values()) {
            counts.put(s, inspections.count(filter(user, s, null, null, null)));
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public InspectionDetail get(AppUser user, UUID id) {
        return InspectionMapper.detail(load(user, id));
    }

    private Specification<Inspection> filter(
            AppUser user, InspectionStatus status, UUID inspectorId, InspectionType type, String q) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Inspection, AppUser> inspector = root.join("inspector");
            Join<Inspection, Contract> contract = root.join("contract");
            UUID effectiveInspector = user.getRole() == Role.INSPECTOR ? user.getId() : inspectorId;
            if (effectiveInspector != null) ps.add(cb.equal(inspector.get("id"), effectiveInspector));
            if (status != null) ps.add(cb.equal(root.get("status"), status));
            if (type != null) ps.add(cb.equal(root.get("type"), type));
            if (q != null && !q.isBlank()) {
                String term = q.trim().toLowerCase(Locale.ROOT);
                String like = "%" + term + "%";
                List<Predicate> any = new ArrayList<>();
                any.add(cb.like(cb.lower(contract.get("clientName")), like));
                any.add(cb.like(cb.lower(inspector.get("name")), like));
                if (term.chars().allMatch(Character::isDigit) && term.length() < 18) {
                    any.add(cb.equal(root.get("number"), Long.parseLong(term)));
                }
                ps.add(cb.or(any.toArray(Predicate[]::new)));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }

    // ------------------------------------------------------------------ gestão

    /** Cria e atribui uma OS: copia os itens do checklist escolhido para a inspeção (snapshot). */
    public InspectionDetail create(AppUser manager, CreateInspectionRequest r) {
        Contract contract = contracts.findById(r.contractId())
                .orElseThrow(() -> ApiException.notFound("CONTRACT_NOT_FOUND", "Contrato não encontrado."));
        ChecklistTemplate template = templates.findById(r.checklistId())
                .orElseThrow(() -> ApiException.notFound("CHECKLIST_NOT_FOUND", "Checklist não encontrado."));
        AppUser inspector = users.findById(r.inspectorId())
                .filter(u -> u.getRole() == Role.INSPECTOR && u.isActive())
                .orElseThrow(() -> ApiException.unprocessable("INVALID_INSPECTOR", "Selecione um inspetor ativo.", null));

        Instant now = clock.instant();
        Inspection i = new Inspection();
        i.setNumber(inspections.nextNumber());
        i.setContract(contract);
        i.setChecklistCode(template.getCode());
        i.setChecklistName(template.getName());
        i.setInspector(inspector);
        i.setManager(manager);
        i.setType(r.type());
        i.setScheduledDate(r.scheduledDate());
        i.setShift(r.shift());
        i.setNotes(blankToNull(r.notes()));
        i.setStatus(InspectionStatus.PENDING);
        i.setCreatedAt(now);
        for (ChecklistTemplateItem t : template.getItems()) {
            InspectionItem item = new InspectionItem();
            item.setInspection(i);
            item.setItemNo(t.getItemNo());
            item.setCategory(t.getCategory());
            item.setStatement(t.getStatement());
            item.setLocation(t.getLocation());
            i.getItems().add(item);
        }
        i.addEvent(EventType.CREATED, manager, now, "OS criada e atribuída a " + inspector.getName(), null);
        return InspectionMapper.detail(inspections.save(i));
    }

    /** Registra o plano de ação de uma não conformidade (somente com a inspeção aguardando revisão). */
    public InspectionDetail addActionPlan(AppUser manager, UUID inspectionId, ActionPlanRequest r) {
        Inspection i = load(manager, inspectionId);
        if (i.getStatus() != InspectionStatus.COMPLETED) {
            throw ApiException.conflict("INVALID_STATE", "Planos de ação só podem ser registrados em inspeções concluídas.");
        }
        InspectionItem item = findItem(i, r.itemId());
        if (!item.isNonConforming()) {
            throw ApiException.unprocessable("ITEM_NOT_NON_CONFORMING", "O plano de ação vale apenas para itens não conformes.", null);
        }
        Instant now = clock.instant();
        ActionPlan plan = new ActionPlan();
        plan.setItem(item);
        plan.setResponsible(r.responsible().trim());
        plan.setDueDate(r.dueDate());
        plan.setAction(r.action().trim());
        plan.setCreatedBy(manager);
        plan.setCreatedAt(now);
        item.getActionPlans().add(plan);
        i.addEvent(EventType.ACTION_PLAN_SAVED, manager, now, "Plano de ação do item " + pad(item.getItemNo()) + " registrado", null);
        return InspectionMapper.detail(inspections.save(i));
    }

    /** Aprova uma inspeção concluída; exige plano de ação para toda NC Alta ou Crítica. */
    public InspectionDetail approve(AppUser manager, UUID inspectionId) {
        Inspection i = load(manager, inspectionId);
        requireCompleted(i);
        List<String> missing = i.getItems().stream()
                .filter(it -> it.isNonConforming() && it.getCriticality() != null && it.getCriticality().requiresActionPlan())
                .filter(it -> it.getActionPlans().isEmpty())
                .map(it -> "Item " + pad(it.getItemNo()) + " (" + it.getCriticality() + ") sem plano de ação")
                .toList();
        if (!missing.isEmpty()) {
            throw ApiException.unprocessable("ACTION_PLAN_REQUIRED",
                    "Registre o plano de ação das não conformidades Alta/Crítica antes de aprovar.", missing);
        }
        Instant now = clock.instant();
        i.setStatus(InspectionStatus.APPROVED);
        i.setReviewedAt(now);
        i.setReviewedBy(manager);
        i.setReviewComment(null);
        i.addEvent(EventType.APPROVED, manager, now, "Inspeção aprovada", null);
        return InspectionMapper.detail(inspections.save(i));
    }

    /** Rejeita e devolve ao inspetor, que verá o motivo no aplicativo. */
    public InspectionDetail reject(AppUser manager, UUID inspectionId, String reason) {
        Inspection i = load(manager, inspectionId);
        requireCompleted(i);
        Instant now = clock.instant();
        i.setStatus(InspectionStatus.REJECTED);
        i.setReviewedAt(now);
        i.setReviewedBy(manager);
        i.setReviewComment(reason.trim());
        i.addEvent(EventType.REJECTED, manager, now, "Rejeitada e devolvida ao inspetor. Motivo: " + reason.trim(), null);
        return InspectionMapper.detail(inspections.save(i));
    }

    // ------------------------------------------------------------------ campo (inspetor)

    /** Marca o início em campo. Idempotente: repetir a chamada em uma OS já iniciada não altera nada. */
    public InspectionDetail start(AppUser inspector, UUID inspectionId, StartRequest r) {
        Inspection i = load(inspector, inspectionId);
        if (i.getStatus() == InspectionStatus.IN_PROGRESS) return InspectionMapper.detail(i);
        if (i.getStatus() != InspectionStatus.PENDING) {
            throw ApiException.conflict("INVALID_STATE", "A inspeção não está pendente.");
        }
        requireLocation(r.location(), "início");
        i.setStatus(InspectionStatus.IN_PROGRESS);
        i.setStartedAt(r.startedAt());
        i.setStartLocation(r.location());
        i.addEvent(EventType.STARTED, inspector, r.startedAt(), "Inspeção iniciada", r.location());
        return InspectionMapper.detail(inspections.save(i));
    }

    /**
     * Recebe uma foto de um item. O {@code photoId} é gerado pelo app: reenviar a mesma foto (retentativa
     * após queda de conexão) não duplica o registro.
     */
    public PhotoDto addPhoto(
            AppUser inspector, UUID inspectionId, UUID photoId, UUID itemId, MultipartFile file, Instant capturedAt,
            GeoPoint location) {
        Inspection i = load(inspector, inspectionId);
        if (!i.getStatus().isEditableByInspector()) {
            throw ApiException.conflict("INVALID_STATE", "A inspeção não aceita mais fotos.");
        }
        InspectionItem item = findItem(i, itemId);
        Optional<Photo> existing = photos.findById(photoId);
        if (existing.isPresent()) {
            return InspectionMapper.photo(existing.get());
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!IMAGE_TYPES.contains(contentType) || file.isEmpty()) {
            throw ApiException.unprocessable("INVALID_IMAGE", "Envie uma imagem JPEG, PNG ou WebP.", null);
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        String ext = contentType.equals("image/png") ? "png" : contentType.equals("image/webp") ? "webp" : "jpg";
        String path = storage.save("photos/" + inspectionId + "/" + photoId + "." + ext, data);

        Photo p = new Photo();
        p.setId(photoId);
        p.setItem(item);
        p.setStoragePath(path);
        p.setContentType(contentType);
        p.setCapturedAt(capturedAt);
        p.setLocation(location);
        item.getPhotos().add(p);
        // persist (e não save): o id é atribuído pelo app, então o Spring Data trataria como "detached" e faria merge.
        em.persist(p);
        return InspectionMapper.photo(p);
    }

    /**
     * Recebe o pacote completo da execução e conclui a inspeção. Pode ser chamado de novo após uma
     * rejeição (correção) e é idempotente para uma OS já concluída. As fotos precisam ter sido enviadas antes.
     */
    public InspectionDetail submit(AppUser inspector, UUID inspectionId, SubmitRequest r) {
        Inspection i = load(inspector, inspectionId);
        if (i.getStatus() == InspectionStatus.COMPLETED) return InspectionMapper.detail(i);
        if (!i.getStatus().isEditableByInspector()) {
            throw ApiException.conflict("INVALID_STATE", "A inspeção já foi revisada e não pode ser reenviada.");
        }

        List<String> problems = new ArrayList<>();
        Map<UUID, ItemAnswer> answers = r.items().stream()
                .collect(Collectors.toMap(ItemAnswer::itemId, a -> a, (a, b) -> b));
        Map<UUID, InspectionItem> byId = i.getItems().stream().collect(Collectors.toMap(InspectionItem::getId, it -> it));
        answers.keySet().stream().filter(id -> !byId.containsKey(id))
                .forEach(id -> problems.add("Item " + id + " não pertence a esta inspeção"));
        for (InspectionItem it : i.getItems()) {
            ItemAnswer a = answers.get(it.getId());
            String label = "Item " + pad(it.getItemNo());
            if (a == null) {
                problems.add(label + " sem resposta");
            } else if (a.result() == ItemResult.NON_CONFORMING) {
                if (a.criticality() == null) problems.add(label + " não conforme sem criticidade");
                if (it.getPhotos().isEmpty()) problems.add(label + " não conforme sem foto");
            }
        }
        if (!r.finishedAt().isAfter(r.startedAt())) problems.add("O encerramento deve ser posterior ao início");
        if (!hasLocation(r.startLocation()) && !hasLocation(i.getStartLocation())) problems.add("Localização de início ausente");
        if (!hasLocation(r.endLocation())) problems.add("Localização de encerramento ausente");
        byte[] signature = decodeSignature(r.signature(), problems);
        if (!problems.isEmpty()) {
            throw ApiException.unprocessable("SUBMISSION_INVALID", "A inspeção não pode ser enviada.", problems);
        }

        for (InspectionItem it : i.getItems()) {
            ItemAnswer a = answers.get(it.getId());
            it.setResult(a.result());
            boolean nc = a.result() == ItemResult.NON_CONFORMING;
            it.setCriticality(nc ? a.criticality() : null);
            it.setDescription(nc ? blankToNull(a.description()) : null);
            it.setRecommendedAction(nc ? blankToNull(a.recommendedAction()) : null);
        }
        Instant now = clock.instant();
        if (i.getStartedAt() == null) {
            i.setStartedAt(r.startedAt());
            i.setStartLocation(r.startLocation());
            i.addEvent(EventType.STARTED, inspector, r.startedAt(), "Inspeção iniciada", r.startLocation());
        }
        i.setFinishedAt(r.finishedAt());
        i.setEndLocation(r.endLocation());
        i.setGeneralNotes(blankToNull(r.generalNotes()));
        i.setSignaturePath(storage.save("signatures/" + i.getId() + ".png", signature));
        i.setStatus(InspectionStatus.COMPLETED);
        i.addEvent(EventType.SUBMITTED, inspector, r.finishedAt(), "Inspeção encerrada e assinada", r.endLocation());
        if (Duration.between(r.finishedAt(), now).toSeconds() > 120) {
            i.addEvent(EventType.SYNCED, inspector, now, "Sincronizada ao reconectar (dados coletados sem internet)", null);
        }
        return InspectionMapper.detail(inspections.save(i));
    }

    // ------------------------------------------------------------------ arquivos

    /** Bytes da assinatura da inspeção, respeitando a visibilidade do usuário. */
    @Transactional(readOnly = true)
    public byte[] signature(AppUser user, UUID inspectionId) {
        Inspection i = load(user, inspectionId);
        if (i.getSignaturePath() == null) throw ApiException.notFound("SIGNATURE_NOT_FOUND", "Assinatura não encontrada.");
        return storage.read(i.getSignaturePath());
    }

    /** Foto com seu tipo de conteúdo, respeitando a visibilidade do usuário sobre a inspeção dona. */
    @Transactional(readOnly = true)
    public StoredFile photoFile(AppUser user, UUID photoId) {
        Photo p = photos.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("PHOTO_NOT_FOUND", "Foto não encontrada."));
        load(user, p.getItem().getInspection().getId());
        return new StoredFile(storage.read(p.getStoragePath()), p.getContentType());
    }

    /** Conteúdo binário de um arquivo armazenado. */
    public record StoredFile(byte[] data, String contentType) {}

    // ------------------------------------------------------------------ utilidades

    /** Carrega a inspeção aplicando a visibilidade: inspetor só acessa as suas (senão 404, sem vazar existência). */
    private Inspection load(AppUser user, UUID id) {
        Inspection i = inspections.findById(id)
                .orElseThrow(() -> ApiException.notFound("INSPECTION_NOT_FOUND", "Inspeção não encontrada."));
        if (user.getRole() == Role.INSPECTOR && !i.getInspector().getId().equals(user.getId())) {
            throw ApiException.notFound("INSPECTION_NOT_FOUND", "Inspeção não encontrada.");
        }
        return i;
    }

    private static InspectionItem findItem(Inspection i, UUID itemId) {
        return i.getItems().stream()
                .filter(it -> it.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("ITEM_NOT_FOUND", "Item não encontrado nesta inspeção."));
    }

    private static void requireCompleted(Inspection i) {
        if (i.getStatus() != InspectionStatus.COMPLETED) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE", "Somente inspeções concluídas podem ser revisadas.");
        }
    }

    private static void requireLocation(GeoPoint g, String label) {
        if (!hasLocation(g)) {
            throw ApiException.unprocessable("LOCATION_REQUIRED", "Localização de " + label + " obrigatória.", null);
        }
    }

    private static boolean hasLocation(GeoPoint g) {
        return g != null && g.isComplete();
    }

    /** Decodifica a assinatura (base64, com ou sem prefixo data-URI) e confere que é um PNG. */
    private static byte[] decodeSignature(String raw, List<String> problems) {
        try {
            String b64 = raw.contains(",") ? raw.substring(raw.indexOf(',') + 1) : raw;
            byte[] data = Base64.getMimeDecoder().decode(b64);
            if (data.length < 8 || !Arrays.equals(Arrays.copyOf(data, 4), PNG_MAGIC)) {
                problems.add("Assinatura deve ser uma imagem PNG");
                return null;
            }
            return data;
        } catch (IllegalArgumentException e) {
            problems.add("Assinatura inválida (base64)");
            return null;
        }
    }

    private static String pad(int n) {
        return String.format("%02d", n);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
