CREATE TABLE user_profiles (
                               user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                               full_name VARCHAR(100),
                               bio TEXT,
                               position VARCHAR(50),
                               age INTEGER,
                               preferred_foot VARCHAR(20),
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for searching users by name (e.g. for "Add to Group")
CREATE INDEX idx_profiles_name ON user_profiles(full_name);