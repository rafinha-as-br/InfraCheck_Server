# InfraCheck_Server

API REST do InfraCheck (Java 21 · Spring Boot 3.5 · PostgreSQL · JWT). Atende o app do inspetor
(`InfraCheck_app_inspetor`) e o painel de gestão (`InfraCheck_app_admin`).

> **Estado: protótipo do MVP.** Cobre o fluxo do design (programar → executar → enviar → revisar → aprovar/rejeitar)
> com validações mínimas. Regras de negócio ainda não definidas, geração de PDF e gestão de cadastros (usuários,
> contratos, formulários) **não** estão implementadas — ver [docs/overview.md](docs/overview.md).

## Rodar

Pré-requisitos: JDK 21 e Maven 3.9+.

**Sem Docker (H2 em arquivo, com dados de demonstração):**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

A API sobe em `http://localhost:8080`. O banco e os arquivos ficam em `./data` (ignorado pelo Git); apague a
pasta para recomeçar do zero. O perfil `local` cria usuários de demonstração — todos com a senha definida em
`DevDataSeeder.DEMO_PASSWORD` (apenas desenvolvimento):

| Papel | E-mail |
| --- | --- |
| Gestora de contrato | `helena.prado@infracheck.com.br` |
| Administrador | `admin@infracheck.com.br` |
| Inspetores | `marcos.oliveira@`, `renata.lopes@`, `diego.antunes@`, `camila.rocha@`, `paulo.sa@`, `juliana.matos@` `infracheck.com.br` |

**Com Docker (PostgreSQL 16 + API):**

```bash
docker compose up --build
```

> A migração (`V1__init.sql`) foi escrita para PostgreSQL e executada nos testes em H2 (modo PostgreSQL).
> A execução contra um PostgreSQL real ainda não foi validada neste protótipo.

## Testes

```bash
mvn test
```

`InspectionFlowTest` percorre o fluxo completo (criar OS → iniciar → enviar fotos → concluir → exigir plano de
ação → aprovar), a visibilidade por papel e o login.

## Configuração

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL local | Conexão com o banco |
| `INFRACHECK_JWT_SECRET` | *(obrigatória, ≥ 32 caracteres)* | Segredo HS256 do JWT |
| `INFRACHECK_STORAGE_PATH` | `./data/storage` | Fotos e assinaturas (dados sensíveis) |
| `INFRACHECK_SEED` | `false` | Dados de demonstração (**nunca em produção**) |
| `PORT` | `8080` | Porta HTTP |

Mais detalhes: [docs/api.md](docs/api.md) (contratos) e [docs/architecture.md](docs/architecture.md) (camadas e decisões).
