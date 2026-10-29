package br.com.infracheck.server.service;

import br.com.infracheck.server.domain.*;
import br.com.infracheck.server.web.dto.Dtos.*;
import java.util.Comparator;
import java.util.List;

/**
 * Conversão entidade → DTO. Deve ser chamado dentro de uma transação (as associações são lazy).
 */
public final class InspectionMapper {

    private InspectionMapper() {}

    public static PersonDto person(AppUser u) {
        return u == null ? null : new PersonDto(u.getId(), u.getName(), u.getRegistration());
    }

    public static ContractDto contract(Contract c) {
        return new ContractDto(
                c.getId(), c.getCode(), c.getClientName(), c.getSegment(), c.getResponsibleName(), c.getAddress());
    }

    /** Totais da inspeção; ver {@link Stats} para a definição de conformidade. */
    public static Stats stats(Inspection i) {
        List<InspectionItem> items = i.getItems();
        int conforming = 0, nonConforming = 0, notApplicable = 0, photos = 0;
        Criticality max = null;
        for (InspectionItem it : items) {
            photos += it.getPhotos().size();
            if (it.getResult() == ItemResult.CONFORMING) conforming++;
            else if (it.getResult() == ItemResult.NOT_APPLICABLE) notApplicable++;
            else if (it.getResult() == ItemResult.NON_CONFORMING) {
                nonConforming++;
                if (it.getCriticality() != null && (max == null || it.getCriticality().compareTo(max) > 0)) {
                    max = it.getCriticality();
                }
            }
        }
        int total = items.size();
        double percent = total == 0 ? 0 : Math.round(conforming * 1000.0 / total) / 10.0;
        return new Stats(total, conforming + nonConforming + notApplicable, conforming, nonConforming, notApplicable,
                photos, max, percent);
    }

    public static InspectionSummary summary(Inspection i) {
        return new InspectionSummary(
                i.getId(),
                i.getNumber(),
                i.getStatus(),
                i.getType(),
                i.getType().isUrgent(),
                i.getScheduledDate(),
                i.getShift(),
                i.getContract().getClientName(),
                i.getContract().getAddress(),
                i.getChecklistCode(),
                person(i.getInspector()),
                i.getManager().getName(),
                i.getStartedAt(),
                i.getFinishedAt(),
                i.getReviewComment(),
                i.getNotes(),
                stats(i));
    }

    public static InspectionDetail detail(Inspection i) {
        return new InspectionDetail(
                i.getId(),
                i.getNumber(),
                i.getStatus(),
                i.getType(),
                i.getType().isUrgent(),
                i.getScheduledDate(),
                i.getShift(),
                i.getNotes(),
                contract(i.getContract()),
                i.getChecklistCode(),
                i.getChecklistName(),
                person(i.getInspector()),
                person(i.getManager()),
                i.getCreatedAt(),
                i.getStartedAt(),
                i.getStartLocation(),
                i.getFinishedAt(),
                i.getEndLocation(),
                i.getGeneralNotes(),
                i.getSignaturePath() != null,
                i.getReviewComment(),
                i.getReviewedAt(),
                i.getReviewedBy() == null ? null : i.getReviewedBy().getName(),
                stats(i),
                i.getItems().stream().map(InspectionMapper::item).toList(),
                i.getEvents().stream()
                        .sorted(Comparator.comparing(InspectionEvent::getOccurredAt))
                        .map(InspectionMapper::event)
                        .toList());
    }

    public static ItemDto item(InspectionItem it) {
        return new ItemDto(
                it.getId(),
                it.getItemNo(),
                it.getCategory(),
                it.getStatement(),
                it.getLocation(),
                it.getResult(),
                it.getCriticality(),
                it.getDescription(),
                it.getRecommendedAction(),
                it.getPhotos().stream().map(InspectionMapper::photo).toList(),
                it.getActionPlans().stream().map(InspectionMapper::actionPlan).toList());
    }

    public static PhotoDto photo(Photo p) {
        GeoPoint g = p.getLocation();
        return new PhotoDto(
                p.getId(), p.getCapturedAt(), g == null ? null : g.lat(), g == null ? null : g.lng(),
                g == null ? null : g.accuracy());
    }

    public static ActionPlanDto actionPlan(ActionPlan a) {
        return new ActionPlanDto(
                a.getId(), a.getResponsible(), a.getDueDate(), a.getAction(), a.getCreatedBy().getName(), a.getCreatedAt());
    }

    public static EventDto event(InspectionEvent e) {
        return new EventDto(
                e.getId(), e.getType(), e.getActor() == null ? null : e.getActor().getName(), e.getOccurredAt(),
                e.getDetail(), e.getLocation());
    }
}
