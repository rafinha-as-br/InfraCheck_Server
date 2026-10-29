# API REST — InfraCheck_Server (`/api/v1`)

Autenticação: `Authorization: Bearer <JWT>` em tudo, exceto `POST /auth/login`. Corpo e respostas em JSON UTF-8
(exceto upload de foto e conteúdo de arquivos). Instantes em ISO-8601 UTC; datas `yyyy-MM-dd`.

Enums: `Role` ADMIN · CONTRACT_MANAGER · INSPECTOR — `InspectionStatus` PENDING · IN_PROGRESS · COMPLETED · APPROVED ·
REJECTED — `InspectionType` PREVENTIVE · CORRECTIVE · PRE_DELIVERY · AUDIT · EMERGENCY — `Shift` MORNING · AFTERNOON ·
NIGHT — `ItemResult` CONFORMING · NON_CONFORMING · NOT_APPLICABLE — `Criticality` LOW · MEDIUM · HIGH · CRITICAL.

## Erros

```json
{ "code": "ACTION_PLAN_REQUIRED", "message": "Registre o plano de ação ...", "details": ["Item 04 (HIGH) sem plano de ação"] }
```

| HTTP | Quando | `code` (exemplos) |
| --- | --- | --- |
| 401 | Sem token, token inválido/expirado, login errado | `INVALID_CREDENTIALS`, `UNKNOWN_USER` |
| 403 | Papel sem permissão para a rota | — |
| 404 | Recurso inexistente **ou** OS de outro inspetor | `INSPECTION_NOT_FOUND`, `ITEM_NOT_FOUND`, `PHOTO_NOT_FOUND` |
| 409 | Estado inválido para a operação | `INVALID_STATE` |
| 413 | Arquivo acima de 15 MB | `FILE_TOO_LARGE` |
| 422 | Validação de campo ou de regra | `VALIDATION_ERROR`, `SUBMISSION_INVALID`, `ACTION_PLAN_REQUIRED`, `LOCATION_REQUIRED`, `INVALID_IMAGE`, `INVALID_INSPECTOR` |

## Autenticação

| Método e rota | Papel | Descrição |
| --- | --- | --- |
| `POST /auth/login` `{email, password}` | público | → `{token, expiresAt, user{id,name,email,role,registration}}` |
| `GET /auth/me` | autenticado | Usuário do token |

## Cadastros de apoio e dashboard (Administrador, Gestor de Contrato)

| Rota | Resposta |
| --- | --- |
| `GET /contracts` | `[{id, code, clientName, segment, responsibleName, address}]` |
| `GET /checklists` | `[{id, code, name, itemCount}]` |
| `GET /inspectors` | `[{id, name, registration, todayCount}]` — carga de OS de hoje |
| `GET /dashboard` | `{inProgress, startedToday, awaitingReview, awaitingReviewOver12h, overdue, avgHoursToApproval, byStatus{}, reviewQueue[], inspectorLoadToday[]}` |

## Inspeções

| Método e rota | Papel | Descrição |
| --- | --- | --- |
| `GET /inspections?status&inspectorId&type&q&page&size` | todos | Página `{content[InspectionSummary], total, page, size}`, mais recentes primeiro. Inspetor recebe só as próprias. `q` busca por nº da OS, cliente ou inspetor. `size` máx. 200. |
| `GET /inspections/counts` | todos | `{PENDING: n, ...}` no mesmo escopo de visibilidade |
| `GET /inspections/{id}` | todos | `InspectionDetail`: contrato, itens (respostas, fotos, planos), localização, eventos, `stats` |
| `GET /inspections/{id}/signature` | todos | PNG da assinatura |
| `GET /photos/{photoId}/content` | todos | Bytes da foto (imagem) |
| `POST /inspections` | gestão | `{contractId, checklistId, inspectorId, type, scheduledDate, shift, notes?}` → `201 InspectionDetail` (status `PENDING`) |
| `POST /inspections/{id}/action-plans` | gestão | `{itemId, responsible, dueDate, action}`; só com a OS `COMPLETED` e item não conforme |
| `POST /inspections/{id}/approve` | gestão | `COMPLETED → APPROVED`; `422 ACTION_PLAN_REQUIRED` se houver NC Alta/Crítica sem plano |
| `POST /inspections/{id}/reject` | gestão | `{reason}`; `COMPLETED → REJECTED` (o inspetor corrige e reenvia) |
| `POST /inspections/{id}/start` | inspetor | `{startedAt, location{lat,lng,accuracy}}`; `PENDING → IN_PROGRESS`; idempotente |
| `PUT /inspections/{id}/photos/{photoId}` | inspetor | `multipart/form-data`: `file`, `itemId`, `capturedAt`, `lat?`, `lng?`, `accuracy?`. `photoId` é um UUID gerado no app; repetir a chamada não duplica |
| `POST /inspections/{id}/submit` | inspetor | Conclui a execução (abaixo); idempotente para OS já `COMPLETED` |

### `POST /inspections/{id}/submit`

```json
{
  "startedAt": "2025-06-12T11:07:00Z",
  "startLocation": {"lat": -23.5613, "lng": -46.6559, "accuracy": 6},
  "finishedAt": "2025-06-12T12:41:00Z",
  "endLocation": {"lat": -23.5614, "lng": -46.6560, "accuracy": 5},
  "generalNotes": "texto livre",
  "signature": "data:image/png;base64,....",
  "items": [
    {"itemId": "…", "result": "CONFORMING"},
    {"itemId": "…", "result": "NON_CONFORMING", "criticality": "HIGH", "description": "…", "recommendedAction": "…"}
  ]
}
```

Regras (violação → `422 SUBMISSION_INVALID`, `details` lista tudo que falta): todos os itens da OS respondidos; item
não conforme com `criticality` e ao menos uma foto **já enviada**; `finishedAt` posterior a `startedAt`; GPS de início
(do envio ou do `start`) e de fim; assinatura PNG. Aceito com a OS em `PENDING`, `IN_PROGRESS` ou `REJECTED`.
Se `finishedAt` for mais de 2 minutos anterior ao recebimento, a trilha ganha um evento `SYNCED`.

## Fluxo offline recomendado ao cliente

1. Baixar a lista e o detalhe (checklist) das OS abertas enquanto houver conexão.
2. Gravar tudo no aparelho durante a execução.
3. Com conexão: `PUT` das fotos pendentes (qualquer ordem, repetível) → `POST submit` (repetível).
4. `422`: corrigir e reenviar; erro de rede: manter na fila e tentar depois.
