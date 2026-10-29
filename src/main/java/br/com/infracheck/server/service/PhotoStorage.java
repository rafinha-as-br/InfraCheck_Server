package br.com.infracheck.server.service;

import br.com.infracheck.server.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

/**
 * Armazenamento em disco de fotos e assinaturas. Fotos, coordenadas e assinaturas são dados sensíveis (LGPD):
 * os arquivos nunca são servidos diretamente — só pelos endpoints autenticados.
 *
 * <p>Os caminhos relativos são montados pelo servidor (UUID + extensão fixa) e a leitura valida que o
 * arquivo resolvido continua dentro da raiz, evitando path traversal.
 *
 * <p>MVP: disco local. Para múltiplas instâncias, trocar por armazenamento de objetos mantendo esta interface.
 */
@Component
public class PhotoStorage {

    private final Path root;

    public PhotoStorage(AppProperties props) {
        this.root = Path.of(props.storage().path()).toAbsolutePath().normalize();
    }

    /** Grava os bytes em {@code relativePath} (criando pastas) e devolve o mesmo caminho relativo. */
    public String save(String relativePath, byte[] data) {
        Path target = resolve(relativePath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            return relativePath;
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gravar " + relativePath, e);
        }
    }

    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + relativePath, e);
        }
    }

    private Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Caminho fora do armazenamento: " + relativePath);
        }
        return p;
    }
}
