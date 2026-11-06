
create table departments (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique,
    name        text not null,
    created_at  timestamptz not null default now()
);

create table users (
    id              uuid primary key default gen_random_uuid(),
    subject         text not null unique,
    email           text not null,
    display_name    text not null,
    department_id   uuid references departments (id),
    created_at      timestamptz not null default now(),
    last_seen_at    timestamptz
);

create index users_department_idx on users (department_id);

create table user_roles (
    user_id  uuid not null references users (id) on delete cascade,
    role     text not null,
    primary key (user_id, role),
    constraint user_roles_role_allowed check (role in (
        'ADMIN', 'OPERATIONS_OFFICER', 'COMPLIANCE_ANALYST',
        'AML_ANALYST', 'SUPERVISOR', 'AUDITOR'
    ))
);

create table audit_events (
    id             uuid primary key default gen_random_uuid(),
    occurred_at    timestamptz not null default now(),
    actor_subject  text not null,
    actor_email    text,
    action         text not null,
    target_type    text,
    target_id      text,
    outcome        text not null default 'SUCCESS',
    details        jsonb not null default '{}'::jsonb,
    correlation_id text
);

create index audit_events_occurred_idx on audit_events (occurred_at desc);
create index audit_events_actor_idx on audit_events (actor_subject, occurred_at desc);

-- Append-only: the database, not only the application, refuses mutation.
create or replace function audit_events_block_mutation()
returns trigger
language plpgsql
as $$
begin
    raise exception 'audit_events is append-only (attempted %)', tg_op
        using errcode = 'restrict_violation';
end;
$$;

create trigger audit_events_append_only
    before update or delete on audit_events
    for each row execute function audit_events_block_mutation();

insert into departments (code, name) values
    ('OPS', 'Banking Operations'),
    ('COMPLIANCE', 'Compliance'),
    ('AML', 'Anti-Money Laundering'),
    ('RISK', 'Risk'),
    ('AUDIT', 'Internal Audit');
