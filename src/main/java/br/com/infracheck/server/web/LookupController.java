package br.com.infracheck.server.web;

import br.com.infracheck.server.domain.Role;
import br.com.infracheck.server.repository.ChecklistTemplateRepository;
import br.com.infracheck.server.repository.ContractRepository;
import br.com.infracheck.server.repository.InspectionRepository;
import br.com.infracheck.server.repository.UserRepository;
import br.com.infracheck.server.service.DashboardService;
import br.com.infracheck.server.service.InspectionMapper;
import br.com.infracheck.server.web.dto.Dtos.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cadastros de apoio do formulário "Nova inspeção" e indicadores do dashboard (apenas gestão). */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LookupController {

    private final ContractRepository contracts;
    private final ChecklistTemplateRepository templates;
    private final UserRepository users;
    private final InspectionRepository inspections;
    private final DashboardService dashboard;
    private final Clock clock;

    @GetMapping("/contracts")
    List<ContractDto> contracts() {
        return contracts.findByActiveTrueOrderByClientName().stream().map(InspectionMapper::contract).toList();
    }

    @Transactional(readOnly = true)
    @GetMapping("/checklists")
    List<ChecklistDto> checklists() {
        return templates.findByActiveTrueOrderByCode().stream()
                .map(t -> new ChecklistDto(t.getId(), t.getCode(), t.getName(), t.getItems().size()))
                .toList();
    }

    /** Inspetores ativos com a quantidade de OS previstas para hoje, para distribuir melhor a carga. */
    @GetMapping("/inspectors")
    List<InspectorDto> inspectors() {
        Map<UUID, Long> load = inspections.findByScheduledDate(LocalDate.now(clock)).stream()
                .collect(Collectors.groupingBy(i -> i.getInspector().getId(), Collectors.counting()));
        return users.findByRoleAndActiveTrueOrderByName(Role.INSPECTOR).stream()
                .map(u -> new InspectorDto(u.getId(), u.getName(), u.getRegistration(), load.getOrDefault(u.getId(), 0L)))
                .toList();
    }

    @GetMapping("/dashboard")
    Dashboard dashboard() {
        return dashboard.dashboard();
    }
}
