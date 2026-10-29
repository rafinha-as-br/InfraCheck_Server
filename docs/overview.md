# Visão geral do InfraCheck_Server

## Responsabilidade

Núcleo de back-end da solução InfraCheck: autenticação, dados, regras de validação, arquivos (fotos e
assinaturas) e trilha de auditoria. É a única fonte de verdade — os dois clientes Flutter não duplicam regra.

## O que existe hoje (protótipo do MVP)

| Capacidade | Estado |
| --- | --- |
| Login por e-mail/senha e JWT (HS256) com papéis Administrador, Gestor de Contrato e Inspetor | Implementado |
| Programar e atribuir OS (cliente/contrato, tipo, checklist, inspetor, data, turno, observações) | Implementado |
| Cópia dos itens do checklist para a OS no momento da criação | Implementado |
| Início em campo com horário e GPS, envio idempotente de fotos e conclusão com assinatura | Implementado |
| Revisão: plano de ação, aprovar, rejeitar com motivo | Implementado |
| Listagem com filtros/paginação, contagem por status, dashboard | Implementado |
| Trilha de auditoria (criação, início, encerramento, sincronização, plano, aprovação/rejeição) | Implementado |
| Dados de demonstração (perfil `local`) | Implementado |
| Cadastro de usuários, contratos/clientes/locais e formulários versionados (CRUD) | **Não implementado** (existem só como dados semeados) |
| Geração do relatório PDF após a aprovação | **Não implementado** |
| Notificações para o app (nova OS, prazo próximo) | **Não implementado** |
| Reabrir/reatribuir OS, cancelar OS, edição de OS | **Não implementado** |
| Validação em PostgreSQL real | Não executada (testes usam H2 em modo PostgreSQL) |

## Validações aplicadas (briefing e Confluence)

- Todo item respondido como `CONFORMING`, `NON_CONFORMING` ou `NOT_APPLICABLE`.
- Não conformidade exige criticidade e ao menos uma foto.
- Início e encerramento com horário e GPS; assinatura em PNG.
- Plano de ação obrigatório para criticidade `HIGH` ou `CRITICAL` antes da aprovação.
- Inspetor enxerga e altera apenas as próprias OS.

Nenhuma outra regra de negócio foi inventada: as demais (prazos, SLA, aprovação em níveis etc.) aguardam definição.

## Simplificações do protótipo

- **Cliente, contrato e local** compartilham a tabela `contract`. Separar quando o modelo for definido.
- **Armazenamento** de fotos/assinaturas em disco local (`PhotoStorage`); trocar por armazenamento de objetos
  para mais de uma instância.
- **Dashboard** calculado em memória a cada chamada; mover para consultas agregadas se o volume crescer.
- O KPI "tempo até o relatório" do design foi substituído por "tempo até a aprovação" (média entre encerramento e
  aprovação), porque ainda não existe PDF.
- Token JWT de 24 h sem refresh token.

## Dados sensíveis (LGPD)

Coordenadas, fotos e assinaturas só são servidas por endpoints autenticados e filtrados por papel/propriedade;
os arquivos nunca são expostos diretamente. Política de retenção, anonimização e consentimento ainda não definidas.
