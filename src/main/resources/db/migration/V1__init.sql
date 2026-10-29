-- InfraCheck Server — esquema inicial do MVP.
-- Escrito para PostgreSQL e também aceito pelo H2 em modo PostgreSQL (perfil "local").
-- Ids são UUIDs gerados pela aplicação.

create table app_user (
    id            uuid primary key,
    name          varchar(120) not null,
    email         varchar(160) not null unique,
    password_hash varchar(100) not null,
    role          varchar(20)  not null,   -- ADMIN | CONTRACT_MANAGER | INSPECTOR
    registration  varchar(20),             -- matrícula do inspetor (ex.: INS-0231)
    active        boolean      not null
);

-- MVP: cliente e contrato num único cadastro (ver docs/api.md, "Simplificações").
create table contract (
    id               uuid primary key,
    code             varchar(30)  not null unique,   -- ex.: CT-2024-117
    client_name      varchar(160) not null,
    segment          varchar(80),
    responsible_name varchar(120),                   -- responsável no local
    address          varchar(240) not null,
    active           boolean      not null
);

create table checklist_template (
    id     uuid primary key,
    code   varchar(40)  not null unique,             -- ex.: CKL-INCENDIO-V3
    name   varchar(160) not null,
    active boolean      not null
);

create table checklist_template_item (
    id          uuid primary key,
    template_id uuid         not null references checklist_template (id),
    item_no     int          not null,
    category    varchar(80)  not null,
    statement   varchar(300) not null,
    location    varchar(160)
);

create sequence inspection_number_seq start with 1059 increment by 1;

create table inspection (
    id               uuid primary key,
    os_number        bigint       not null unique,
    contract_id      uuid         not null references contract (id),
    checklist_code   varchar(40)  not null,          -- cópia do template no momento da criação
    checklist_name   varchar(160) not null,
    inspector_id     uuid         not null references app_user (id),
    manager_id       uuid         not null references app_user (id),
    type             varchar(20)  not null,
    scheduled_date   date         not null,
    shift            varchar(10)  not null,
    notes            varchar(1000),
    status           varchar(20)  not null,
    started_at       timestamp with time zone,
    start_lat        double precision,
    start_lng        double precision,
    start_accuracy   double precision,
    finished_at      timestamp with time zone,
    end_lat          double precision,
    end_lng          double precision,
    end_accuracy     double precision,
    general_notes    varchar(2000),
    signature_path   varchar(255),
    review_comment   varchar(1000),
    reviewed_at      timestamp with time zone,
    reviewed_by      uuid references app_user (id),
    created_at       timestamp with time zone not null
);
create index idx_inspection_status on inspection (status);
create index idx_inspection_inspector on inspection (inspector_id);

-- Itens são copiados do template ao criar a OS: mudanças futuras no checklist
-- não alteram inspeções já programadas.
create table inspection_item (
    id                 uuid primary key,
    inspection_id      uuid         not null references inspection (id),
    item_no            int          not null,
    category           varchar(80)  not null,
    statement          varchar(300) not null,
    location           varchar(160),
    result             varchar(20),                  -- CONFORMING | NON_CONFORMING | NOT_APPLICABLE
    criticality        varchar(10),                  -- só para NON_CONFORMING
    description        varchar(2000),
    recommended_action varchar(1000)
);
create index idx_item_inspection on inspection_item (inspection_id);

create table inspection_photo (
    id           uuid primary key,                   -- gerado pelo app (idempotência no envio offline)
    item_id      uuid         not null references inspection_item (id),
    storage_path varchar(255) not null,
    content_type varchar(60)  not null,
    captured_at  timestamp with time zone not null,
    lat          double precision,
    lng          double precision,
    accuracy     double precision
);
create index idx_photo_item on inspection_photo (item_id);

create table action_plan (
    id          uuid primary key,
    item_id     uuid          not null references inspection_item (id),
    responsible varchar(120)  not null,
    due_date    date          not null,
    action_text varchar(1000) not null,
    created_by  uuid          not null references app_user (id),
    created_at  timestamp with time zone not null
);
create index idx_plan_item on action_plan (item_id);

-- Trilha de auditoria (append-only): quem, quando, onde e o quê.
create table inspection_event (
    id            uuid primary key,
    inspection_id uuid         not null references inspection (id),
    type          varchar(30)  not null,
    actor_id      uuid         references app_user (id),
    occurred_at   timestamp with time zone not null,
    detail        varchar(500),
    lat           double precision,
    lng           double precision,
    accuracy      double precision
);
create index idx_event_inspection on inspection_event (inspection_id);
