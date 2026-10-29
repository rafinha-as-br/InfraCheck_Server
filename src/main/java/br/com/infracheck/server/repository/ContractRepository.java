package br.com.infracheck.server.repository;

import br.com.infracheck.server.domain.Contract;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, UUID> {

    List<Contract> findByActiveTrueOrderByClientName();
}
