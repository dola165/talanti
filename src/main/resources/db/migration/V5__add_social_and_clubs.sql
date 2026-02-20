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