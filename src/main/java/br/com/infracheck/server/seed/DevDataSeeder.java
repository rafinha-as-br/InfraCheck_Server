package br.com.infracheck.server.seed;

import br.com.infracheck.server.domain.*;
import br.com.infracheck.server.repository.*;
import br.com.infracheck.server.service.PhotoStorage;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dados de DEMONSTRAÇÃO para o protótipo (perfil {@code local}, {@code infracheck.seed=true}).
 *
 * <p>Só roda com o banco vazio. Reproduz o cenário do design — gestora, seis inspetores, clientes,
 * OS em todos os estados — com datas relativas a "hoje". Fotos e assinaturas são imagens geradas
 * (placeholders). <strong>Nunca habilitar em produção</strong>: todos os usuários compartilham a
 * senha {@value #DEMO_PASSWORD}.
 */
@Component
@ConditionalOnProperty(name = "infracheck.seed", havingValue = "true")
@RequiredArgsConstructor
public class DevDataSeeder implements ApplicationRunner {

    /** Senha dos usuários de demonstração (somente desenvolvimento). */
    public static final String DEMO_PASSWORD = "infracheck";

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);
    private static final double BASE_LAT = -23.5613;
    private static final double BASE_LNG = -46.6559;

    private final UserRepository users;
    private final ContractRepository contracts;
    private final ChecklistTemplateRepository templates;
    private final InspectionRepository inspections;
    private final PasswordEncoder encoder;
    private final PhotoStorage storage;
    private final Clock clock;

    /** Não conformidade a registrar em uma OS semeada. */
    private record Nc(Criticality criticality, String description, String action) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) return;
        System.setProperty("java.awt.headless", "true");

        AppUser admin = user("Administrador InfraCheck", "admin@infracheck.com.br", Role.ADMIN, null);
        AppUser helena = user("Helena Prado", "helena.prado@infracheck.com.br", Role.CONTRACT_MANAGER, null);
        AppUser marcos = user("Marcos Oliveira", "marcos.oliveira@infracheck.com.br", Role.INSPECTOR, "INS-0231");
        AppUser renata = user("Renata Lopes", "renata.lopes@infracheck.com.br", Role.INSPECTOR, "INS-0198");
        AppUser diego = user("Diego Antunes", "diego.antunes@infracheck.com.br", Role.INSPECTOR, "INS-0207");
        AppUser camila = user("Camila Rocha", "camila.rocha@infracheck.com.br", Role.INSPECTOR, "INS-0215");
        AppUser paulo = user("Paulo H. Sá", "paulo.sa@infracheck.com.br", Role.INSPECTOR, "INS-0242");
        user("Juliana Matos", "juliana.matos@infracheck.com.br", Role.INSPECTOR, "INS-0256");

        Contract bella = contract("CT-2024-117", "Condomínio Bella Vista", "Gestão condominial", "Sônia Ribeiro (síndica)", "Av. Paulista, 1200 — São Paulo/SP");
        Contract serra = contract("CT-2023-044", "Frigorífico Serra Azul", "Indústria alimentícia", "Álvaro Teixeira", "Rod. Dom Pedro I, km 62 — Atibaia/SP");
        Contract jaguari = contract("CT-2022-009", "Estação Elevatória Jaguari", "Saneamento", "Marta Fonseca", "Rod. Anhanguera, km 29 — Osasco/SP");
        Contract clara = contract("CT-2024-081", "Edifício Santa Clara", "Gestão condominial", "Roberto Nunes", "R. Augusta, 2210 — São Paulo/SP");
        Contract vertice = contract("CT-2023-120", "Distribuidora Vértice Logística", "Logística", "Fernanda Gil", "Rod. Anchieta, km 14 — São Bernardo do Campo/SP");
        Contract vila = contract("CT-2024-066", "Subestação Vila Nova", "Energia", "Cláudio Reis", "R. Itapura, 880 — Santo André/SP");
        Contract aurora = contract("CT-2023-097", "Condomínio Residencial Aurora", "Gestão condominial", "Beatriz Lima", "Av. Ipiranga, 344 — São Paulo/SP");
        Contract torre = contract("CT-2024-140", "Torre Norte Corporate", "Corporativo", "Henrique Dias", "Av. Brig. Faria Lima, 3477 — São Paulo/SP");
        Contract acacias = contract("CT-2024-092", "Condomínio Parque das Acácias", "Gestão condominial", "Lúcia Prado", "R. das Acácias, 480 — Barueri/SP");
        Contract rica = contract("CT-2023-051", "Condomínio Vila Rica", "Gestão condominial", "Otávio Brandão", "R. Vergueiro, 1520 — São Paulo/SP");

        ChecklistTemplate fire = template("CKL-INCENDIO-V3", "Sistemas de incêndio e segurança", new String[][] {
            {"Sistema de incêndio", "Extintor ABC 6 kg — carga dentro do prazo de validade", "Hall Térreo"},
            {"Sistema de incêndio", "Extintor ABC 6 kg — manômetro na faixa verde", "Hall Térreo"},
            {"Sistema de incêndio", "Extintor CO₂ 6 kg — lacre íntegro", "Sala de Servidores"},
            {"Sistema de incêndio", "Mangueira de incêndio — sem ressecamento ou trincas", "Abrigo Bloco A — 3º andar"},
            {"Sistema de incêndio", "Iluminação de emergência — acionamento em teste manual", "Corredor Principal"},
            {"Sistema de incêndio", "Hidrante — registro abre e fecha livremente", "Abrigo Bloco A — térreo"},
            {"Sistema de incêndio", "Hidrante — esguicho e chave de mangueira presentes", "Abrigo Bloco A — térreo"},
            {"Sistema de incêndio", "Detector de fumaça — teste de acionamento", "Corredor Principal"},
            {"Sistema de incêndio", "Alarme de incêndio — acionador manual", "Hall Térreo"},
            {"Civil e áreas comuns", "Sinalização de saída de emergência legível", "Corredor B — 2º subsolo"},
            {"CFTV e segurança", "Câmera de CFTV com imagem nítida", "Subsolo 1 — câmera 7"},
            {"CFTV e segurança", "Gravador de CFTV com retenção mínima de 30 dias", "Sala de Segurança"},
        });
        ChecklistTemplate electric = template("CKL-ELETRICA-V2", "Instalações elétricas", new String[][] {
            {"Elétrica", "Quadro geral — fechamento e identificação dos circuitos", "Subsolo — quadro geral"},
            {"Elétrica", "Disjuntores — sem sinais de aquecimento", "Subsolo — quadro geral"},
            {"Elétrica", "Aterramento — continuidade verificada", "Casa de máquinas"},
            {"Elétrica", "DPS — indicador de estado normal", "Subsolo — quadro geral"},
            {"Elétrica", "Cabeamento — isolação íntegra e sem emendas expostas", "Shaft elétrico"},
            {"Elétrica", "Tomadas e iluminação das áreas comuns em funcionamento", "Áreas comuns"},
            {"Elétrica", "SPDA — conexões e captores visíveis e íntegros", "Cobertura"},
            {"Elétrica", "Gerador — nível de combustível e partida", "Casa de máquinas"},
        });

        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();
        Instant at = today.atStartOfDay(clock.getZone()).toInstant();

        // Concluídas em dias anteriores
        Inspection i1031 = newInspection(1031, aurora, fire, marcos, helena, InspectionType.PREVENTIVE, today.minusDays(3), Shift.MORNING, null, at.minus(Duration.ofDays(4)));
        execute(i1031, marcos, at.minus(Duration.ofDays(3)).plus(Duration.ofHours(8)), at.minus(Duration.ofDays(3)).plus(Duration.ofHours(9)), Map.of());
        approve(i1031, helena, at.minus(Duration.ofDays(3)).plus(Duration.ofHours(14)));

        Inspection i1038 = newInspection(1038, serra, electric, marcos, helena, InspectionType.AUDIT, today.minusDays(2), Shift.AFTERNOON, null, at.minus(Duration.ofDays(3)));
        execute(i1038, marcos, at.minus(Duration.ofDays(2)).plus(Duration.ofHours(14)), at.minus(Duration.ofDays(2)).plus(Duration.ofHours(15)),
                Map.of(3, new Nc(Criticality.MEDIUM, "Medição de aterramento sem registro legível.", "Refazer a medição.")));
        reject(i1038, helena, at.minus(Duration.ofDays(2)).plus(Duration.ofHours(16)), "A foto do item 03 está ilegível. Refaça a foto e reenvie.");

        Inspection i1043 = newInspection(1043, torre, electric, paulo, helena, InspectionType.PRE_DELIVERY, today.minusDays(1), Shift.MORNING, null, at.minus(Duration.ofDays(3)));
        execute(i1043, paulo, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(8)), at.minus(Duration.ofDays(1)).plus(Duration.ofHours(9)), Map.of());
        approve(i1043, helena, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(15)));

        Inspection i1045 = newInspection(1045, aurora, electric, camila, helena, InspectionType.CORRECTIVE, today.minusDays(1), Shift.MORNING, null, at.minus(Duration.ofDays(3)));
        execute(i1045, camila, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(9)), at.minus(Duration.ofDays(1)).plus(Duration.ofHours(10)),
                Map.of(4, new Nc(Criticality.LOW, "Indicador do DPS sem identificação.", "Identificar o dispositivo.")));
        approve(i1045, helena, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(16)));

        // Aguardando revisão (fila do gestor)
        Inspection i1042 = newInspection(1042, rica, electric, marcos, helena, InspectionType.PREVENTIVE, today.minusDays(1), Shift.AFTERNOON, null, at.minus(Duration.ofDays(3)));
        execute(i1042, marcos, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(16)), at.minus(Duration.ofDays(1)).plus(Duration.ofHours(17)).plus(Duration.ofMinutes(50)),
                Map.of(5, new Nc(Criticality.LOW, "Cabo com isolação ressecada.", "Substituir o trecho.")));

        Inspection i1044 = newInspection(1044, vertice, fire, marcos, helena, InspectionType.PREVENTIVE, today.minusDays(1), Shift.AFTERNOON, null, at.minus(Duration.ofDays(3)));
        execute(i1044, marcos, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(13)), at.minus(Duration.ofDays(1)).plus(Duration.ofHours(15)).plus(Duration.ofMinutes(10)), Map.of(
                2, new Nc(Criticality.MEDIUM, "Ponteiro do manômetro na faixa vermelha.", "Solicitar recarga do extintor."),
                4, new Nc(Criticality.HIGH, "Mangueira com trinca no encaixe.", "Substituir a mangueira."),
                10, new Nc(Criticality.LOW, "Placa desbotada, difícil de ler a mais de 5 m.", "Substituir a placa de sinalização."),
                11, new Nc(Criticality.MEDIUM, "Câmera 7 sem imagem no gravador.", "Agendar revisão da câmera e do cabeamento.")));

        Inspection i1050 = newInspection(1050, acacias, fire, camila, helena, InspectionType.PREVENTIVE, today.minusDays(1), Shift.AFTERNOON, null, at.minus(Duration.ofDays(3)));
        execute(i1050, camila, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(15)), at.minus(Duration.ofDays(1)).plus(Duration.ofHours(17)).plus(Duration.ofMinutes(32)), Map.of());

        Instant recent = now.minus(Duration.ofHours(3));
        Inspection i1051 = newInspection(1051, vila, fire, diego, helena, InspectionType.PREVENTIVE, today, Shift.MORNING, null, at.minus(Duration.ofDays(2)));
        execute(i1051, diego, recent.minus(Duration.ofMinutes(100)), recent,
                Map.of(8, new Nc(Criticality.MEDIUM, "Detector sem resposta ao teste de fumaça.", "Substituir o detector.")));

        // Em andamento e pendentes (o app do Marcos mostra estas)
        Inspection i1046 = newInspection(1046, clara, fire, marcos, helena, InspectionType.PREVENTIVE, today.minusDays(1), Shift.AFTERNOON, null, at.minus(Duration.ofDays(2)));
        startOnly(i1046, marcos, at.minus(Duration.ofDays(1)).plus(Duration.ofHours(16)).plus(Duration.ofMinutes(40)));
        Inspection i1048 = newInspection(1048, vertice, electric, renata, helena, InspectionType.PREVENTIVE, today, Shift.MORNING, null, at.minus(Duration.ofDays(2)));
        startOnly(i1048, renata, at.plus(Duration.ofHours(8)));

        newInspection(1047, bella, fire, marcos, helena, InspectionType.PREVENTIVE, today, Shift.MORNING,
                "Verificar o CFTV do subsolo. O cliente pediu atenção especial ao quadro elétrico do subsolo.", now.minus(Duration.ofDays(2)));
        newInspection(1049, jaguari, electric, marcos, helena, InspectionType.EMERGENCY, today, Shift.AFTERNOON, "Falha de energia reportada pelo cliente.", now.minus(Duration.ofHours(5)));
        newInspection(1052, serra, electric, marcos, helena, InspectionType.AUDIT, today, Shift.AFTERNOON, null, now.minus(Duration.ofDays(1)));
        newInspection(1055, torre, fire, marcos, helena, InspectionType.PRE_DELIVERY, today.plusDays(1), Shift.MORNING, null, now.minus(Duration.ofDays(1)));
        newInspection(1058, acacias, electric, marcos, helena, InspectionType.PREVENTIVE, today.plusDays(4), Shift.MORNING, null, now.minus(Duration.ofHours(8)));

        log.info("Dados de demonstração criados (admin: {}, gestora: {}, inspetor: {}).", admin.getEmail(), helena.getEmail(), marcos.getEmail());
    }

    // ------------------------------------------------------------------ montagem

    private AppUser user(String name, String email, Role role, String registration) {
        AppUser u = new AppUser();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(DEMO_PASSWORD));
        u.setRole(role);
        u.setRegistration(registration);
        return users.save(u);
    }

    private Contract contract(String code, String client, String segment, String responsible, String address) {
        Contract c = new Contract();
        c.setCode(code);
        c.setClientName(client);
        c.setSegment(segment);
        c.setResponsibleName(responsible);
        c.setAddress(address);
        return contracts.save(c);
    }

    private ChecklistTemplate template(String code, String name, String[][] items) {
        ChecklistTemplate t = new ChecklistTemplate();
        t.setCode(code);
        t.setName(name);
        for (int n = 0; n < items.length; n++) {
            ChecklistTemplateItem it = new ChecklistTemplateItem();
            it.setTemplate(t);
            it.setItemNo(n + 1);
            it.setCategory(items[n][0]);
            it.setStatement(items[n][1]);
            it.setLocation(items[n][2]);
            t.getItems().add(it);
        }
        return templates.save(t);
    }

    private Inspection newInspection(long number, Contract c, ChecklistTemplate t, AppUser inspector, AppUser manager,
            InspectionType type, LocalDate date, Shift shift, String notes, Instant createdAt) {
        Inspection i = new Inspection();
        i.setNumber(number);
        i.setContract(c);
        i.setChecklistCode(t.getCode());
        i.setChecklistName(t.getName());
        i.setInspector(inspector);
        i.setManager(manager);
        i.setType(type);
        i.setScheduledDate(date);
        i.setShift(shift);
        i.setNotes(notes);
        i.setStatus(InspectionStatus.PENDING);
        i.setCreatedAt(createdAt);
        for (ChecklistTemplateItem ti : t.getItems()) {
            InspectionItem it = new InspectionItem();
            it.setInspection(i);
            it.setItemNo(ti.getItemNo());
            it.setCategory(ti.getCategory());
            it.setStatement(ti.getStatement());
            it.setLocation(ti.getLocation());
            i.getItems().add(it);
        }
        i.addEvent(EventType.CREATED, manager, createdAt, "OS criada e atribuída a " + inspector.getName(), null);
        return inspections.save(i);
    }

    private void startOnly(Inspection i, AppUser inspector, Instant start) {
        GeoPoint g = point(0);
        i.setStatus(InspectionStatus.IN_PROGRESS);
        i.setStartedAt(start);
        i.setStartLocation(g);
        i.addEvent(EventType.STARTED, inspector, start, "Inspeção iniciada", g);
    }

    /** Responde todos os itens (conformes, exceto os de {@code ncs}, indexados pelo nº do item) e conclui a OS. */
    private void execute(Inspection i, AppUser inspector, Instant start, Instant end, Map<Integer, Nc> ncs) {
        startOnly(i, inspector, start);
        for (InspectionItem it : i.getItems()) {
            Nc nc = ncs.get(it.getItemNo());
            if (nc == null) {
                it.setResult(ItemResult.CONFORMING);
                continue;
            }
            it.setResult(ItemResult.NON_CONFORMING);
            it.setCriticality(nc.criticality());
            it.setDescription(nc.description());
            it.setRecommendedAction(nc.action());
            Photo p = new Photo();
            p.setId(UUID.randomUUID());
            p.setItem(it);
            p.setContentType("image/png");
            p.setCapturedAt(start.plus(Duration.ofMinutes(10L * it.getItemNo())));
            p.setLocation(point(it.getItemNo()));
            p.setStoragePath(storage.save("photos/%s/%s.png".formatted(i.getId() == null ? i.getNumber() : i.getId(), p.getId()),
                    placeholder("Foto de demonstração · item " + it.getItemNo(), 640, 480)));
            it.getPhotos().add(p);
        }
        i.setFinishedAt(end);
        i.setEndLocation(point(1));
        i.setGeneralNotes("Vistoria concluída conforme o checklist.");
        i.setSignaturePath(storage.save("signatures/os-" + i.getNumber() + ".png", signature()));
        i.setStatus(InspectionStatus.COMPLETED);
        i.addEvent(EventType.SUBMITTED, inspector, end, "Inspeção encerrada e assinada", i.getEndLocation());
    }

    private void approve(Inspection i, AppUser manager, Instant at) {
        i.setStatus(InspectionStatus.APPROVED);
        i.setReviewedAt(at);
        i.setReviewedBy(manager);
        i.addEvent(EventType.APPROVED, manager, at, "Inspeção aprovada", null);
    }

    private void reject(Inspection i, AppUser manager, Instant at, String reason) {
        i.setStatus(InspectionStatus.REJECTED);
        i.setReviewedAt(at);
        i.setReviewedBy(manager);
        i.setReviewComment(reason);
        i.addEvent(EventType.REJECTED, manager, at, "Rejeitada e devolvida ao inspetor. Motivo: " + reason, null);
    }

    private static GeoPoint point(int jitter) {
        return new GeoPoint(BASE_LAT - jitter * 0.00001, BASE_LNG - jitter * 0.00001, 6.0);
    }

    // ------------------------------------------------------------------ imagens de demonstração

    private static byte[] placeholder(String label, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(0x0B2B4D), w, h, new Color(0x1560AB)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 40));
        g.setStroke(new BasicStroke(6));
        int m = 24, c = 56; // cantoneiras de "moldura de inspeção"
        g.drawLine(m, m, m + c, m); g.drawLine(m, m, m, m + c);
        g.drawLine(w - m, m, w - m - c, m); g.drawLine(w - m, m, w - m, m + c);
        g.drawLine(m, h - m, m + c, h - m); g.drawLine(m, h - m, m, h - m - c);
        g.drawLine(w - m, h - m, w - m - c, h - m); g.drawLine(w - m, h - m, w - m, h - m - c);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
        g.drawString(label, (w - g.getFontMetrics().stringWidth(label)) / 2, h / 2);
        g.dispose();
        return png(img);
    }

    private static byte[] signature() {
        BufferedImage img = new BufferedImage(400, 150, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x0F1B2A));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Double();
        p.moveTo(30, 100);
        p.curveTo(60, 20, 90, 20, 110, 90);
        p.curveTo(130, 140, 160, 30, 200, 70);
        p.curveTo(230, 100, 260, 40, 300, 80);
        p.curveTo(320, 100, 340, 60, 370, 70);
        g.draw(p);
        g.dispose();
        return png(img);
    }

    private static byte[] png(BufferedImage img) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
