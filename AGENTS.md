# InfraCheck Server — contexto para agentes

## Propósito

Este repositório contém o núcleo de back-end da solução InfraCheck. Ele recebe, valida, processa e armazena a operação de inspeções, expondo uma API REST segura para o aplicativo de inspetores e para o painel de gestão.

O requisito contratado é Java com Spring Boot, PostgreSQL e Docker, com comunicação HTTPS. Consulte `../../AGENTS.md` e os documentos de origem apontados nele antes de decidir regras de domínio ou contratos.

## Responsabilidades

- autenticação JWT e controle de acesso para Administrador, Gestor de Contrato e Inspetor;
- cadastro e gestão de usuários, contratos, clientes, locais, formulários e checklists versionados;
- criação, atribuição e ciclo de vida de inspeções;
- recebimento seguro de respostas, fotos, localização, horários e assinaturas originados no app de campo;
- preservação do histórico auditável de inspeções e alterações;
- revisão, aprovação/rejeição e planos de ação para não conformidades;
- geração automática do relatório PDF depois da aprovação;
- disponibilização de dados consistentes para o painel web e para o app móvel.

## Regras críticas já conhecidas

- Cada item deve ser concluído como `Conforme`, `Não Conforme` ou `Não Aplicável`.
- Uma inspeção percorre pelo menos os estados `Pendente`, `Em Andamento`, `Concluída`, `Aprovada` e `Rejeitada`.
- Inspeções aprovadas devem ter todos os itens respondidos, geolocalização de início e fim, horários reais de execução e assinatura digital do inspetor.
- Não conformidades precisam de evidência fotográfica; itens Alta ou Crítica exigem plano de ação antes da aprovação.
- O PDF é uma saída automática dos dados aprovados, não um documento manualmente editado.

## Cuidados de implementação

Geolocalização, fotos, assinaturas, dados de colaboradores e de clientes demandam controles coerentes com a LGPD. A trilha de auditoria é requisito funcional, não um detalhe opcional.

Evite acoplar a API a uma interface específica. Contratos devem permitir o funcionamento offline do app do inspetor, incluindo envio posterior e seguro dos dados coletados. Não introduza serviços pagos de terceiros em funcionalidades centrais, sobretudo na geração de PDF.

O repositório está em estado inicial: não presuma modelo de dados, endpoints, ferramenta de migração ou padrões internos antes de eles existirem e serem documentados.
