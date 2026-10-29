package br.com.infracheck.server.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Foto-evidência vinculada a um item. O id é gerado pelo app, o que torna o reenvio offline idempotente.
 * O arquivo fica no armazenamento em disco; aqui ficam só o caminho e os metadados de captura.
 */
@Entity
@Table(name = "inspection_photo")
@Getter
@Setter
@NoArgsConstructor
public class Photo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private InspectionItem item;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    /** Momento real da captura no aparelho (não o do upload). */
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Embedded
    private GeoPoint location;
}
