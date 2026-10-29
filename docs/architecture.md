# Arquitetura do InfraCheck_Server

## Camadas

```
br.com.infracheck.server
├── config/      AppProperties (infracheck.*), SecurityConfig (JWT, CORS, Clock)
├── domain/      Entidades JPA + enums (Inspection, InspectionItem, Photo, ActionPlan, InspectionEvent ...)
├── repository/  Spring Data JPA (Inspection usa Specification para os filtros)
├── service/     InspectionService (casos de uso), DashboardService, AuthService, PhotoStorage, InspectionMapper
├── web/         Controllers REST, ApiException + ApiExceptionHandler, dto/Dtos (contratos JSON)
└── seed/        DevDataSeeder (somente perfil local / infracheck.seed=true)
```

- `web` → `service` → `repository` → `domain`. DTOs ficam em `web/dto` e **nunca** são entidades.
- Mapeamento entidade → DTO em `InspectionMapper`, dentro de transação (associações lazy,
  `open-in-view=false`).
- Erros de negócio: `ApiException(status, code, message, details)` → corpo `{code, message, details[]}`.

## Segurança

- Spring Security stateless; `oauth2-resource-server` valida o JWT HS256. A claim `role` vira `ROLE_<role>`.
- Autorização **por rota e papel** em `SecurityConfig`; autorização **por registro** em `InspectionService.load`
  (inspetor vê só as próprias OS; para ele uma OS alheia responde 404, sem revelar a existência).
- Login devolve a mesma mensagem para e-mail inexistente, senha errada e usuário inativo.
- CORS limitado a `infracheck.cors.allowed-origin-patterns` (padrão: localhost).

## Banco de dados

Flyway (`db/migration/V1__init.sql`); Hibernate em `ddl-auto: validate`. Ids UUID gerados pela aplicação; número da OS
vem da sequência `inspection_number_seq`. Tabelas: `app_user`, `contract`, `checklist_template(_item)`, `inspection`,
`inspection_item`, `inspection_photo`, `action_plan`, `inspection_event`.

## Decisões relevantes

| Decisão | Motivo |
| --- | --- |
| Itens do checklist **copiados** para a OS | Mudar um checklist não altera inspeções já programadas (versionamento). |
| Envio offline em **dois passos**: fotos (`PUT` com id do app) e depois `submit` | Idempotência: retentativas após queda de conexão não duplicam nada. |
| `submit` valida tudo e devolve `422 SUBMISSION_INVALID` com a lista do que falta | O app mostra o motivo exato sem lógica duplicada. |
| Eventos de auditoria com horário do **aparelho** e evento `SYNCED` com horário do servidor quando o envio atrasa | Preserva "quando aconteceu" e "quando chegou". |
| Enums em inglês nas respostas | Contrato estável; rótulos em português ficam nos clientes. |
| Perfil `local` com H2 | Rodar o protótipo sem Docker; produção usa PostgreSQL. |

## Como evoluir

1. Nova regra de negócio → `InspectionService` + teste em `InspectionFlowTest` + `docs/api.md`.
2. Nova coluna/tabela → nova migração `V<n>__*.sql` (nunca editar a `V1`).
3. Novo endpoint → controller, regra de papel em `SecurityConfig`, DTO em `Dtos`, `docs/api.md`.
