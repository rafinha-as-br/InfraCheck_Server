package br.com.infracheck.server.repository;

import br.com.infracheck.server.domain.ChecklistTemplate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChecklistTemplateRepository extends JpaRepository<ChecklistTemplate, UUID> {

    List<ChecklistTemplate> findByActiveTrueOrderByCode();
}
