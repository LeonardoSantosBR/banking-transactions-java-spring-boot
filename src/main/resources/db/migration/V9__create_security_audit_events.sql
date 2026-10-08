CREATE TABLE security_audit_events (
    id                      UUID          DEFAULT gen_random_uuid() PRIMARY KEY,
    event_type              VARCHAR (80)  NOT NULL,
    actor_user_id           UUID         ,
    subject_user_id         UUID         ,
    subject_identifier_hmac VARCHAR (64) ,
    source_ip               VARCHAR (45)         ,
    user_agent              VARCHAR (512),
    occurred_at             TIMESTAMPTZ   DEFAULT now() NOT NULL,
    CONSTRAINT fk_security_audit_events_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_security_audit_events_subject FOREIGN KEY (subject_user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX idx_security_audit_events_occurred_at
    ON security_audit_events(occurred_at);

CREATE INDEX idx_security_audit_events_type_occurred_at
    ON security_audit_events(event_type, occurred_at);

CREATE INDEX idx_security_audit_events_actor_occurred_at
    ON security_audit_events(actor_user_id, occurred_at) WHERE actor_user_id IS NOT NULL;

CREATE INDEX idx_security_audit_events_subject_occurred_at
    ON security_audit_events(subject_user_id, occurred_at) WHERE subject_user_id IS NOT NULL;

CREATE INDEX idx_security_audit_events_identifier_occurred_at
    ON security_audit_events(subject_identifier_hmac, occurred_at) WHERE subject_identifier_hmac IS NOT NULL;
