package br.com.infracheck.server.repository;

import br.com.infracheck.server.domain.AppUser;
import br.com.infracheck.server.domain.Role;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, UUID> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findByRoleAndActiveTrueOrderByName(Role role);
}
