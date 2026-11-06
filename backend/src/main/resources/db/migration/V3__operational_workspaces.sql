-- Explicit supervisory scope: the Risk supervisor reviews the three operational departments.
create table department_review_scopes (
 supervisor_department text not null references departments(code),
 resource_department text not null references departments(code),
 primary key(supervisor_department,resource_department)
);
insert into department_review_scopes values ('RISK','OPS'),('RISK','COMPLIANCE'),('RISK','AML');
create table operation_cases (
 id uuid primary key, title varchar(180) not null, kind varchar(20) not null check(kind in ('OPERATIONS','COMPLIANCE','AML')),
 description text not null, synthetic_reference varchar(80) not null check(synthetic_reference like 'SYN-%'),
 department_code text not null references departments(code), creator_subject text not null,
 status varchar(20) not null default 'OPEN' check(status in ('OPEN','IN_REVIEW','RETURNED','CLOSED')),
 version bigint not null default 0, created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index operation_cases_scope on operation_cases(department_code,status,created_at desc);
create table case_events (
 id uuid primary key, case_id uuid not null references operation_cases(id), actor_subject text not null,
 action varchar(40) not null, note text not null, created_at timestamptz not null default now()
);
create index case_events_case on case_events(case_id,created_at);
create trigger case_events_append_only before update or delete on case_events for each row execute function audit_events_block_mutation();
create trigger case_events_no_truncate before truncate on case_events for each statement execute function audit_events_block_mutation();
create table documents (
 id uuid primary key, case_id uuid references operation_cases(id), purpose varchar(10) not null check(purpose in ('CASE','POLICY')),
 filename varchar(180) not null, object_key text not null unique, sha256 char(64) not null, size_bytes bigint not null,
 owner_subject text not null, department_code text not null references departments(code),
 status varchar(20) not null default 'QUEUED' check(status in ('QUEUED','PROCESSING','READY','FAILED','BLOCKED')),
 extracted_fields jsonb not null default '{}'::jsonb, error_message text,
 created_at timestamptz not null default now(),
 constraint document_scope check ((purpose='CASE' and case_id is not null) or (purpose='POLICY' and case_id is null))
);
create index documents_scope on documents(department_code,created_at desc);
create table processing_jobs (
 id uuid primary key, document_id uuid not null unique references documents(id),
 status varchar(16) not null default 'QUEUED' check(status in ('QUEUED','RUNNING','DONE','FAILED')),
 attempts integer not null default 0, available_at timestamptz not null default now(), locked_until timestamptz,
 lease_token uuid, last_error text, created_at timestamptz not null default now()
);
create index processing_jobs_claim on processing_jobs(status,available_at);
create table knowledge_chunks (
 id uuid primary key, document_id uuid not null references documents(id), ordinal integer not null,
 content text not null, search_vector tsvector generated always as (to_tsvector('simple',content)) stored,
 unique(document_id,ordinal)
);
create index knowledge_search on knowledge_chunks using gin(search_vector);
create table ai_executions (
 id uuid primary key, actor_subject text not null, operation text not null,
 prompt_version text not null, model text not null, status text not null,
 duration_ms bigint not null, token_usage bigint, created_at timestamptz not null default now()
);
create index ai_executions_actor on ai_executions(actor_subject,created_at desc);
create table policy_answers (
 id uuid primary key, actor_subject text not null, question text not null, answer text not null,
 abstained boolean not null, execution_id uuid not null references ai_executions(id), created_at timestamptz not null default now()
);
create table answer_citations (
 answer_id uuid not null references policy_answers(id), chunk_id uuid not null references knowledge_chunks(id),
 primary key(answer_id,chunk_id)
);
create table aml_simulations (
 id uuid primary key, actor_subject text not null, department_code text not null references departments(code),
 created_at timestamptz not null default now()
);
create table synthetic_transactions (
 id uuid primary key, simulation_id uuid not null references aml_simulations(id), account_reference text not null check(account_reference like 'SYN-%'),
 amount numeric(18,2) not null check(amount>0), currency char(3) not null, occurred_at timestamptz not null
);
create table aml_alerts (
 id uuid primary key, simulation_id uuid not null references aml_simulations(id), case_id uuid not null references operation_cases(id),
 account_reference text not null, rule_code text not null, rationale text not null, created_at timestamptz not null default now(),
 unique(simulation_id,account_reference,rule_code)
);
