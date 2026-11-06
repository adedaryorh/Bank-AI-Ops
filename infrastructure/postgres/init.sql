-- BankOps AI: database bootstrap.
-- Flyway owns the schema; this file only enables extensions the schema needs.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
