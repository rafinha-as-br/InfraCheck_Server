package br.com.infracheck.server.service;

import br.com.infracheck.server.domain.*;
import br.com.infracheck.server.repository.InspectionRepository;
import br.com.infracheck.server.web.dto.Dtos.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicadores do painel de gestão. Calculados sob demanda a partir das inspeções (volume do MVP é
 * pequeno); se a base crescer, mover para consultas agregadas no banco.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    /** Tempo de espera a partir do qual uma revisão pendente é destacada. */
    private static final Duration REVIEW_ALERT = Duration.ofHours(12);

    private final InspectionRepository inspections;
    private final Clock clock;

    public Dashboard dashboard() {
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        Instant startOfToday = today.atStartOfDay(clock.getZone()).toInstant();

        List<Inspection> open = inspections.findByStatusIn(List.of(InspectionStatus.PENDING, InspectionStatus.IN_PROGRESS));
        long inProgress = open.stream().filter(i -> i.getStatus() == InspectionStatus.IN_PROGRESS).count();
        long startedToday = open.stream()
                .filter(i -> i.getStartedAt() != null && !i.getStartedAt().isBefore(startOfToday)).count();
        long overdue = open.stream().filter(i -> i.getScheduledDate().isBefore(today)).count();

        List<Inspection> queue = inspections.findByStatusOrderByFinishedAtAsc(InspectionStatus.COMPLETED);
        long over12h = queue.stream()
                .filter(i -> i.getFinishedAt() != null && Duration.between(i.getFinishedAt(), now).compareTo(REVIEW_ALERT) > 0)
                .count();
        List<ReviewQueueEntry> reviewQueue = queue.stream()
                .map(i -> new ReviewQueueEntry(InspectionMapper.summary(i),
                        i.getFinishedAt() == null ? 0 : Duration.between(i.getFinishedAt(), now).toMinutes()))
                .toList();

        Double avgHours = inspections.findByStatusIn(List.of(InspectionStatus.APPROVED)).stream()
                .filter(i -> i.getFinishedAt() != null && i.getReviewedAt() != null)
                .mapToDouble(i -> Duration.between(i.getFinishedAt(), i.getReviewedAt()).toMinutes() / 60.0)
                .average().stream().boxed().findFirst().map(v -> Math.round(v * 10) / 10.0).orElse(null);

        Map<InspectionStatus, Long> byStatus = new EnumMap<>(InspectionStatus.class);
        for (InspectionStatus s : InspectionStatus.values()) byStatus.put(s, inspections.countByStatus(s));

        Map<UUID, List<Inspection>> todayByInspector = inspections.findByScheduledDate(today).stream()
                .collect(Collectors.groupingBy(i -> i.getInspector().getId()));
        List<InspectorLoad> load = todayByInspector.values().stream()
                .map(list -> new InspectorLoad(list.get(0).getInspector().getId(), list.get(0).getInspector().getName(), list.size()))
                .sorted(Comparator.comparingLong(InspectorLoad::count).reversed().thenComparing(InspectorLoad::name))
                .toList();

        return new Dashboard(inProgress, startedToday, queue.size(), over12h, overdue, avgHours, byStatus, reviewQueue, load);
    }
}
