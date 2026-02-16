
CREATE TABLE message_reads (
                               message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (message_id, user_id)
);

-- 7. MEDIA (Files)
CREATE TABLE media (
                       id BIGSERIAL PRIMARY KEY,
                       url TEXT NOT NULL,
                       type VARCHAR(50),
                       size_bytes BIGINT,
                       uploaded_by BIGINT REFERENCES users(id),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. NOTIFICATIONS
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type VARCHAR(30) NOT NULL,
                               entity_type VARCHAR(30),
                               entity_id BIGINT,
                               title VARCHAR(255),
                               body TEXT,
                               is_read BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Speed

CREATE INDEX idx_notifications_user ON notifications(user_id);