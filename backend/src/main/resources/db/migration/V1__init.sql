CREATE TABLE app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT chk_app_users_role CHECK (role IN ('ADMIN', 'AGENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE leads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(30) NULL,
    company VARCHAR(160) NULL,
    source VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    assigned_agent_id BIGINT NOT NULL,
    notes TEXT NULL,
    lost_reason VARCHAR(500) NULL,
    archived TINYINT(1) NOT NULL DEFAULT 0,
    archived_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_leads_agent FOREIGN KEY (assigned_agent_id) REFERENCES app_users (id),
    CONSTRAINT chk_leads_contact CHECK (email IS NOT NULL OR phone IS NOT NULL),
    CONSTRAINT chk_leads_source CHECK (source IN ('WEBSITE', 'PHONE', 'REFERRAL', 'SOCIAL_MEDIA', 'OTHER')),
    CONSTRAINT chk_leads_status CHECK (status IN ('NEW', 'CONTACTED', 'QUALIFIED', 'LOST')),
    CONSTRAINT chk_leads_lost_reason CHECK (status <> 'LOST' OR lost_reason IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_leads_agent_archived_status ON leads (assigned_agent_id, archived, status);
CREATE INDEX idx_leads_status ON leads (status);

CREATE TABLE deals (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    lead_id BIGINT NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    stage VARCHAR(20) NOT NULL,
    expected_close_date DATE NOT NULL,
    closing_reason VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_deals_lead UNIQUE (lead_id),
    CONSTRAINT fk_deals_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT chk_deals_amount CHECK (amount > 0),
    CONSTRAINT chk_deals_stage CHECK (stage IN ('PROPOSAL', 'NEGOTIATION', 'WON', 'LOST')),
    CONSTRAINT chk_deals_lost_reason CHECK (stage <> 'LOST' OR closing_reason IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_deals_stage ON deals (stage);

CREATE TABLE activities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    author_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_activities_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_activities_author FOREIGN KEY (author_id) REFERENCES app_users (id),
    CONSTRAINT chk_activities_type CHECK (type IN ('NOTE', 'CALL', 'MEETING', 'EMAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_activities_lead ON activities (lead_id, created_at);

CREATE TABLE follow_ups (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    description VARCHAR(2000) NOT NULL,
    due_at DATETIME(6) NOT NULL,
    completed TINYINT(1) NOT NULL DEFAULT 0,
    completed_at DATETIME(6) NULL,
    created_by_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_follow_ups_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_follow_ups_author FOREIGN KEY (created_by_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_follow_ups_due ON follow_ups (completed, due_at);

CREATE TABLE lead_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    lead_id BIGINT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    detail VARCHAR(500) NOT NULL,
    actor_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_lead FOREIGN KEY (lead_id) REFERENCES leads (id),
    CONSTRAINT fk_history_actor FOREIGN KEY (actor_id) REFERENCES app_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_history_lead ON lead_history (lead_id, created_at);
