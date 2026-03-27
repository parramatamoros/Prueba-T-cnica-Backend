-- ============================================================
-- V1__init_schema.sql
-- Schema inicial
-- ============================================================

CREATE TABLE IF NOT EXISTS user_role (
    id          BIGSERIAL       PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    email       VARCHAR(100)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    enabled     BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS task (
    id          BIGSERIAL       PRIMARY KEY,
    title       VARCHAR(200)    NOT NULL,
    description TEXT,
    status      VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    user_id     BIGINT          NOT NULL REFERENCES user_role(id) ON DELETE CASCADE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Índices para búsquedas frecuentes
CREATE INDEX idx_task_user_id   ON task(user_id);
CREATE INDEX idx_task_status    ON task(status);
CREATE INDEX idx_task_user_status ON task(user_id, status);

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_updated_at
    BEFORE UPDATE ON user_role
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_task_updated_at
    BEFORE UPDATE ON task
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Usuario ADMIN inicial (password: Admin123!)
INSERT INTO user_role (username, email, password, role)
VALUES (
    'admin',
    'admin@admin.com',
    '$2a$12$SKYRFHUGZIuYbRgb44CswO7eiXIHBJ0Nd7ut9gZm40or3VA5C4qru',
    'ADMIN'
) ON CONFLICT DO NOTHING;
