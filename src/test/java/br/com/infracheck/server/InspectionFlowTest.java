package br.com.infracheck.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.infracheck.server.seed.DevDataSeeder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Fluxo ponta a ponta contra o banco em memória com os dados de demonstração:
 * gestor cria a OS → inspetor inicia, envia fotos e conclui → gestor exige plano de ação, aprova.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:flow;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
    "infracheck.storage.path=target/test-storage"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
class InspectionFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fluxoCompletoDaCriacaoAAprovacao() throws Exception {
        String manager = login("helena.prado@infracheck.com.br");
        String inspector = login("marcos.oliveira@infracheck.com.br");

        // Gestor cria e atribui a OS
        JsonNode contract = getJson(manager, "/api/v1/contracts").get(0);
        JsonNode checklist = getJson(manager, "/api/v1/checklists").get(1);
        JsonNode marcos = null;
        for (JsonNode n : getJson(manager, "/api/v1/inspectors")) if (n.get("name").asText().startsWith("Marcos")) marcos = n;
        JsonNode created = postJson(manager, "/api/v1/inspections", Map.of(
                "contractId", contract.get("id").asText(), "checklistId", checklist.get("id").asText(),
                "inspectorId", marcos.get("id").asText(), "type", "PREVENTIVE",
                "scheduledDate", LocalDate.now().plusDays(2).toString(), "shift", "MORNING"), 201);
        String id = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("PENDING");
        assertThat(created.get("items")).hasSize(12);

        // Inspetor não pode criar OS nem ver o painel
        mvc.perform(post("/api/v1/inspections").header("Authorization", "Bearer " + inspector)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());

        // Inspetor inicia (idempotente) e envia foto do item não conforme
        Map<String, Object> start = Map.of("startedAt", Instant.now().minusSeconds(3600).toString(),
                "location", Map.of("lat", -23.5613, "lng", -46.6559, "accuracy", 6.0));
        assertThat(postJson(inspector, "/api/v1/inspections/" + id + "/start", start, 200).get("status").asText()).isEqualTo("IN_PROGRESS");
        postJson(inspector, "/api/v1/inspections/" + id + "/start", start, 200);

        JsonNode detail = getJson(inspector, "/api/v1/inspections/" + id);
        List<Map<String, Object>> answers = new ArrayList<>();
        String ncItem = detail.get("items").get(0).get("id").asText();
        for (JsonNode item : detail.get("items")) {
            boolean nc = item.get("id").asText().equals(ncItem);
            answers.add(nc
                    ? Map.of("itemId", ncItem, "result", "NON_CONFORMING", "criticality", "HIGH", "description", "Trinca", "recommendedAction", "Trocar")
                    : Map.of("itemId", item.get("id").asText(), "result", "CONFORMING"));
        }
        Map<String, Object> submit = Map.of("startedAt", Instant.now().minusSeconds(3600).toString(),
                "finishedAt", Instant.now().toString(), "endLocation", Map.of("lat", -23.5614, "lng", -46.656),
                "signature", "data:image/png;base64," + Base64.getEncoder().encodeToString(png()), "items", answers);

        // Sem foto na NC o envio é recusado, com a lista do que falta
        mvc.perform(post("/api/v1/inspections/" + id + "/submit").header("Authorization", "Bearer " + inspector)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(submit)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SUBMISSION_INVALID"))
                .andExpect(jsonPath("$.details[0]").value("Item 01 não conforme sem foto"));

        UUID photoId = UUID.randomUUID();
        for (int tentativa = 0; tentativa < 2; tentativa++) { // reenvio idempotente
            mvc.perform(multipart("/api/v1/inspections/" + id + "/photos/" + photoId)
                            .file(new MockMultipartFile("file", "f.png", "image/png", png()))
                            .param("itemId", ncItem).param("capturedAt", Instant.now().toString()).param("lat", "-23.5613")
                            .with(rq -> { rq.setMethod("PUT"); return rq; })
                            .header("Authorization", "Bearer " + inspector))
                    .andExpect(status().isOk());
        }
        postJson(inspector, "/api/v1/inspections/" + id + "/submit", submit, 200);
        JsonNode done = getJson(manager, "/api/v1/inspections/" + id);
        assertThat(done.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(done.get("stats").get("photos").asInt()).isEqualTo(1);

        // Aprovação bloqueada até existir plano de ação para NC Alta
        mvc.perform(post("/api/v1/inspections/" + id + "/approve").header("Authorization", "Bearer " + manager))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("ACTION_PLAN_REQUIRED"));
        postJson(manager, "/api/v1/inspections/" + id + "/action-plans", Map.of("itemId", ncItem, "responsible", "Síndica",
                "dueDate", LocalDate.now().plusDays(7).toString(), "action", "Trocar mangueira"), 201);
        assertThat(postJson(manager, "/api/v1/inspections/" + id + "/approve", null, 200).get("status").asText()).isEqualTo("APPROVED");

        // Foto acessível ao gestor; trilha de auditoria completa
        String photo = done.get("items").get(0).get("photos").get(0).get("id").asText();
        mvc.perform(get("/api/v1/photos/" + photo + "/content").header("Authorization", "Bearer " + manager)).andExpect(status().isOk());
        assertThat(getJson(manager, "/api/v1/inspections/" + id).get("events")).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void inspetorNaoVeOsDeOutroInspetor() throws Exception {
        String marcos = login("marcos.oliveira@infracheck.com.br");
        String renata = login("renata.lopes@infracheck.com.br");
        JsonNode mine = getJson(marcos, "/api/v1/inspections?size=100").get("content");
        assertThat(mine).isNotEmpty();
        String marcosOs = mine.get(0).get("id").asText();
        mvc.perform(get("/api/v1/inspections/" + marcosOs).header("Authorization", "Bearer " + renata)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/inspections")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginComSenhaErradaFalha() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"helena.prado@infracheck.com.br\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ------------------------------------------------------------------ helpers

    private String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", DevDataSeeder.DEMO_PASSWORD))))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode getJson(String token, String url) throws Exception {
        MvcResult r = mvc.perform(get(url).header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private JsonNode postJson(String token, String url, Object body, int expected) throws Exception {
        var rq = post(url).header("Authorization", "Bearer " + token);
        if (body != null) rq = rq.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        MvcResult r = mvc.perform(rq).andExpect(status().is(expected)).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private static byte[] png() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }
}
