CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(255) UNIQUE, -- Email might be optional if phone is used
                       phone_number VARCHAR(20) UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,

    -- 0 = User, 1 = Admin, etc. (We map this in Java later)
                       system_role SMALLINT NOT NULL DEFAULT 0,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_seen TIMESTAMP
);

-- Optional: Create an index on system_role if you plan to filter by "Admins" often
CREATE INDEX idx_users_role ON users(system_role);

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


-- Security & Auth
CREATE TABLE oauth2_logins (
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               provider VARCHAR(50) NOT NULL,
                               provider_id VARCHAR(255) NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (user_id, provider)
);

-- Core Entities
CREATE TABLE locations (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           address VARCHAR(255),
                           latitude DECIMAL(10, 8) NOT NULL,
                           longitude DECIMAL(11, 8) NOT NULL,
                           type VARCHAR(20)
);

CREATE TABLE clubs (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
                       type VARCHAR(20) NOT NULL,
                       is_official BOOLEAN DEFAULT FALSE,
                       created_by BIGINT NOT NULL REFERENCES users(id),
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Social Graph
CREATE TABLE follows (
                         follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         PRIMARY KEY (follower_id, following_id)
);

CREATE TABLE club_follows (
                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (user_id, club_id)
);

CREATE TABLE club_memberships (
                                  club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  role VARCHAR(50) NOT NULL,
                                  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (club_id, user_id)
);

-- Content & Feed
CREATE TABLE posts (
                       id BIGSERIAL PRIMARY KEY,
                       author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       club_id BIGINT REFERENCES clubs(id) ON DELETE CASCADE,
                       content TEXT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       is_public BOOLEAN DEFAULT TRUE
);

CREATE TABLE post_media (
                            post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                            media_id BIGINT NOT NULL REFERENCES media(id) ON DELETE CASCADE,
                            display_order INTEGER DEFAULT 0,
                            PRIMARY KEY (post_id, media_id)
);

CREATE TABLE likes (
                       post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (post_id, user_id)
);

CREATE TABLE comments (
                          id BIGSERIAL PRIMARY KEY,
                          post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                          user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          content TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE post_seen (
                           post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                           seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (post_id, user_id)
);

-- Tryouts
CREATE TABLE tryouts (
                         id BIGSERIAL PRIMARY KEY,
                         club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                         title VARCHAR(255) NOT NULL,
                         description TEXT,
                         position VARCHAR(50),
                         age_group VARCHAR(50),
                         location_id BIGINT REFERENCES locations(id),
                         tryout_date TIMESTAMP NOT NULL,
                         deadline TIMESTAMP,
                         created_by BIGINT NOT NULL REFERENCES users(id),
                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tryout_applications (
                                     id BIGSERIAL PRIMARY KEY,
                                     tryout_id BIGINT NOT NULL REFERENCES tryouts(id) ON DELETE CASCADE,
                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                     message TEXT,
                                     applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     reviewed_at TIMESTAMP,
                                     reviewed_by BIGINT REFERENCES users(id)
);