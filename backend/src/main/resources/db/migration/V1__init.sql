CREATE TABLE skill (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX uk_skill_name ON skill (lower(name));

CREATE TABLE task (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    effort_hours INT          NOT NULL CHECK (effort_hours BETWEEN 1 AND 1000),
    status       VARCHAR(10)  NOT NULL CHECK (status IN ('DONE', 'NOT_DONE')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE task_skill (
    task_id  BIGINT NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skill (id),
    PRIMARY KEY (task_id, skill_id)
);

CREATE TABLE worker (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(120) NOT NULL,
    availability_hours_per_day INT          NOT NULL CHECK (availability_hours_per_day BETWEEN 1 AND 24),
    created_at                 TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE worker_skill (
    worker_id BIGINT NOT NULL REFERENCES worker (id) ON DELETE CASCADE,
    skill_id  BIGINT NOT NULL REFERENCES skill (id),
    PRIMARY KEY (worker_id, skill_id)
);

CREATE TABLE assignment (
    id         BIGSERIAL PRIMARY KEY,
    worker_id  BIGINT      NOT NULL REFERENCES worker (id) ON DELETE CASCADE,
    task_id    BIGINT      NOT NULL REFERENCES task (id) ON DELETE CASCADE,
    start_date DATE        NOT NULL,
    end_date   DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_assignment_task UNIQUE (task_id),
    CONSTRAINT ck_assignment_dates CHECK (end_date >= start_date)
);

CREATE INDEX ix_assignment_worker ON assignment (worker_id);

CREATE TABLE assignment_allocation (
    assignment_id BIGINT NOT NULL REFERENCES assignment (id) ON DELETE CASCADE,
    work_date     DATE   NOT NULL,
    hours         INT    NOT NULL CHECK (hours BETWEEN 1 AND 24),
    PRIMARY KEY (assignment_id, work_date)
);

CREATE TABLE app_user (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(10)  NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    CONSTRAINT uk_app_user_username UNIQUE (username)
);
