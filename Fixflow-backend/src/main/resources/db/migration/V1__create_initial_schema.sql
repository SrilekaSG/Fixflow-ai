CREATE TABLE knowledge_articles (
    id                 VARCHAR(64)  PRIMARY KEY,
    title              VARCHAR(200) NOT NULL,
    category           VARCHAR(30)  NOT NULL,
    keywords           TEXT         NOT NULL,
    body               TEXT         NOT NULL,
    diagnostic_steps   TEXT         NOT NULL,
    verification_steps TEXT         NOT NULL,
    source_label       VARCHAR(100) NOT NULL DEFAULT 'Built-in FixFlow guide',
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    search_vector      TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', coalesce(keywords, '')), 'B') ||
        setweight(to_tsvector('english', coalesce(body, '')), 'C')
    ) STORED,
    CONSTRAINT chk_article_category CHECK (category IN
        ('DATABASE', 'CONFIGURATION', 'NULL_POINTER', 'DEPENDENCY', 'HTTP', 'BUILD', 'UNKNOWN'))
);

CREATE INDEX idx_knowledge_articles_search ON knowledge_articles USING GIN (search_vector);

CREATE TABLE diagnoses (
    id               UUID         PRIMARY KEY,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    title            VARCHAR(200),
    sanitized_input  TEXT         NOT NULL,
    file_name        VARCHAR(255),
    exception_class  VARCHAR(300),
    error_message    TEXT,
    category         VARCHAR(30)  NOT NULL DEFAULT 'UNKNOWN',
    severity         VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    diagnosis_json   TEXT,
    analysis_mode    VARCHAR(20)  NOT NULL,
    provider_name    VARCHAR(100),
    model_name       VARCHAR(100),
    status           VARCHAR(20)  NOT NULL,
    redaction_count  INTEGER      NOT NULL DEFAULT 0,
    feedback_rating  VARCHAR(20),
    feedback_comment VARCHAR(500),
    CONSTRAINT chk_diagnosis_category CHECK (category IN
        ('DATABASE', 'CONFIGURATION', 'NULL_POINTER', 'DEPENDENCY', 'HTTP', 'BUILD', 'UNKNOWN')),
    CONSTRAINT chk_diagnosis_severity CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'UNKNOWN')),
    CONSTRAINT chk_diagnosis_status CHECK (status IN ('COMPLETED', 'PARTIAL', 'FAILED')),
    CONSTRAINT chk_diagnosis_mode CHECK (analysis_mode IN ('AI', 'RULE_BASED')),
    CONSTRAINT chk_feedback_rating CHECK (feedback_rating IS NULL OR feedback_rating IN ('HELPFUL', 'NOT_HELPFUL'))
);

CREATE INDEX idx_diagnoses_created_at ON diagnoses (created_at DESC);

CREATE TABLE diagnosis_matched_articles (
    diagnosis_id UUID        NOT NULL REFERENCES diagnoses (id) ON DELETE CASCADE,
    article_id   VARCHAR(64) NOT NULL REFERENCES knowledge_articles (id),
    PRIMARY KEY (diagnosis_id, article_id)
);