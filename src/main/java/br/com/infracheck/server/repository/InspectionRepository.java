package br.com.infracheck.server.repository;

import br.com.infracheck.server.domain.Inspection;
import br.com.infracheck.server.domain.InspectionStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InspectionRepository extends JpaRepository<Inspection, UUID>, JpaSpecificationExecutor<Inspection> {

    /** Próximo número de OS (sequência do banco, segura sob concorrência). */
    @Query(value = "select nextval('inspection_number_seq')", nativeQuery = true)
    long nextNumber();

    List<Inspection> findByStatusOrderByFinishedAtAsc(InspectionStatus status);

    List<Inspection> findByStatusIn(Collection<InspectionStatus> statuses);

    List<Inspection> findByScheduledDate(LocalDate date);

    long countByStatus(InspectionStatus status);
}
