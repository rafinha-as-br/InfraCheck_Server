package br.com.infracheck.server.repository;

import br.com.infracheck.server.domain.Photo;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {}
