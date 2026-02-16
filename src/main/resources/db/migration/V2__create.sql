-- 1. Conversations (Groups or Private chats)
CREATE TABLE conversations (
                               id BIGSERIAL PRIMARY KEY,
                               name VARCHAR(100), -- Optional: for named group chats
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Participants (Who is in which conversation)
CREATE TABLE conversation_participants (
                                           conversation_id BIGINT NOT NULL REFERENCES conversations(id),
                                           user_id BIGINT NOT NULL REFERENCES users(id),
                                           joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (conversation_id, user_id)
);

-- 3. Messages (The actual chats)
CREATE TABLE messages (
                          id BIGSERIAL PRIMARY KEY,
                          conversation_id BIGINT NOT NULL REFERENCES conversations(id),
                          sender_id BIGINT NOT NULL REFERENCES users(id),
                          content TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_messages_conversation ON messages(conversation_id);