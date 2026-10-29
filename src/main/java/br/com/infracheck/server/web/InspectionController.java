package br.com.infracheck.server.web;

import br.com.infracheck.server.domain.*;
import br.com.infracheck.server.service.AuthService;
import br.com.infracheck.server.service.InspectionService;
import br.com.infracheck.server.web.dto.Dtos.*;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Endpoints de inspeção. A permissão por papel está em {@code SecurityConfig}; a visibilidade por
 * registro (inspetor só vê as suas) está em {@link InspectionService}. Documentação: {@code docs/api.md}.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InspectionController {

    private final InspectionService service;
    private final AuthService auth;

    private AppUser user(Jwt jwt) {
        return auth.currentUser(jwt.getSubject());
    }

    // ---- consulta (todos os papéis autenticados)

    /** Lista paginada, mais recentes primeiro. {@code q} busca por número da OS, cliente ou inspetor. */
    @GetMapping("/inspections")
    PageDto<InspectionSummary> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) InspectionStatus status,
            @RequestParam(required = false) UUID inspectorId,
            @RequestParam(required = false) InspectionType type,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "number"));
        return service.list(user(jwt), status, inspectorId, type, q, pageable);
    }

    /** Quantidade de inspeções por status (chips de filtro). */
    @GetMapping("/inspections/counts")
    Map<InspectionStatus, Long> counts(@AuthenticationPrincipal Jwt jwt) {
        return service.countByStatus(user(jwt));
    }

    @GetMapping("/inspections/{id}")
    InspectionDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(user(jwt), id);
    }

    @GetMapping(value = "/inspections/{id}/signature", produces = MediaType.IMAGE_PNG_VALUE)
    byte[] signature(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.signature(user(jwt), id);
    }

    @GetMapping("/photos/{photoId}/content")
    ResponseEntity<byte[]> photo(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID photoId) {
        InspectionService.StoredFile f = service.photoFile(user(jwt), photoId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).body(f.data());
    }

    // ---- gestão

    @PostMapping("/inspections")
    @ResponseStatus(HttpStatus.CREATED)
    InspectionDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateInspectionRequest r) {
        return service.create(user(jwt), r);
    }

    @PostMapping("/inspections/{id}/action-plans")
    @ResponseStatus(HttpStatus.CREATED)
    InspectionDetail addActionPlan(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ActionPlanRequest r) {
        return service.addActionPlan(user(jwt), id, r);
    }

    @PostMapping("/inspections/{id}/approve")
    InspectionDetail approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.approve(user(jwt), id);
    }

    @PostMapping("/inspections/{id}/reject")
    InspectionDetail reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody RejectRequest r) {
        return service.reject(user(jwt), id, r.reason());
    }

    // ---- campo (inspetor)

    @PostMapping("/inspections/{id}/start")
    InspectionDetail start(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody StartRequest r) {
        return service.start(user(jwt), id, r);
    }

    /** Upload idempotente de foto (multipart). {@code photoId} é um UUID gerado pelo app. */
    @PutMapping(value = "/inspections/{id}/photos/{photoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    PhotoDto uploadPhoto(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @PathVariable UUID photoId,
            @RequestParam UUID itemId,
            @RequestParam Instant capturedAt,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double accuracy,
            @RequestPart("file") MultipartFile file) {
        return service.addPhoto(user(jwt), id, photoId, itemId, file, capturedAt, new GeoPoint(lat, lng, accuracy));
    }

    @PostMapping("/inspections/{id}/submit")
    InspectionDetail submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody SubmitRequest r) {
        return service.submit(user(jwt), id, r);
    }
}
