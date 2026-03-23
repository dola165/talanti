-- ==========================================
-- 1. CORE IDENTITY & AUTHENTICATION
-- ==========================================
CREATE TABLE IF NOT EXISTS users (
                                     id BIGSERIAL PRIMARY KEY,
                                     username VARCHAR(50) NOT NULL UNIQUE,
                                     email VARCHAR(255) UNIQUE,
                                     phone_number VARCHAR(20) UNIQUE,
                                     password_hash VARCHAR(255),
                                     user_type VARCHAR(30) NOT NULL DEFAULT 'FAN', -- 'FAN', 'PLAYER', 'AGENT', 'CLUB_ADMIN', 'SYSTEM_ADMIN'
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     last_seen TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_users_type ON users(user_type);

CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           jti VARCHAR(128) NOT NULL UNIQUE,
                                           user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                           expires_at TIMESTAMP NOT NULL,
                                           revoked BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_auth_tokens_jti ON auth_tokens(jti);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     token VARCHAR(128) NOT NULL UNIQUE,
                                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                     expires_at TIMESTAMP NOT NULL,
                                                     used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS oauth2_logins (
                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             provider VARCHAR(50) NOT NULL,
                                             provider_id VARCHAR(255) NOT NULL,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (user_id, provider)
);

-- ==========================================
-- 2. MEDIA
-- ==========================================
CREATE TABLE IF NOT EXISTS media (
                                     id BIGSERIAL PRIMARY KEY,
                                     url TEXT NOT NULL,
                                     type VARCHAR(50),
                                     size_bytes BIGINT,
                                     uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 3. PROFILES & DOMAIN EXTENSIONS
-- ==========================================
CREATE TABLE IF NOT EXISTS user_profiles (
                                             user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                             full_name VARCHAR(100),
                                             bio TEXT,
                                             profile_picture_url VARCHAR(255),
                                             banner_url VARCHAR(255),
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_profiles_name ON user_profiles(full_name);

CREATE TABLE IF NOT EXISTS player_details (
                                              user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                              primary_position VARCHAR(50),
                                              secondary_position VARCHAR(50),
                                              preferred_foot VARCHAR(20),
                                              height_cm INTEGER,
                                              weight_kg INTEGER,
                                              date_of_birth DATE,
                                              availability_status VARCHAR(50) DEFAULT 'AVAILABLE', -- 'AVAILABLE', 'IN_CLUB', 'INJURED'
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_details (
                                             user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                                             agency_name VARCHAR(150),
                                             fifa_license_number VARCHAR(100) UNIQUE,
                                             is_verified BOOLEAN DEFAULT FALSE,
                                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS player_career_history (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                     club_name VARCHAR(100) NOT NULL,
                                                     season VARCHAR(20) NOT NULL,
                                                     category VARCHAR(50),
                                                     appearances INTEGER DEFAULT 0,
                                                     goals INTEGER DEFAULT 0,
                                                     assists INTEGER DEFAULT 0,
                                                     clean_sheets INTEGER DEFAULT 0,
                                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 4. MAP & LOCATIONS (Polymorphic)
-- ==========================================
CREATE TABLE IF NOT EXISTS locations (
                                         id BIGSERIAL PRIMARY KEY,
                                         address_text VARCHAR(255),
                                         latitude DECIMAL(10, 8) NOT NULL,
                                         longitude DECIMAL(11, 8) NOT NULL,
                                         created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_locations_lat_lng ON locations(latitude, longitude);

-- ==========================================
-- 5. THE PROTAGONISTS: CLUBS & ORGANIZATIONS
-- ==========================================
CREATE TABLE IF NOT EXISTS clubs (
                                     id BIGSERIAL PRIMARY KEY,
                                     name VARCHAR(255) NOT NULL,
                                     description TEXT,
                                     founded_year INTEGER,
                                     type VARCHAR(50) NOT NULL, -- 'PROFESSIONAL', 'GRASSROOTS', 'ACADEMY'
                                     status VARCHAR(50) DEFAULT 'UNVERIFIED', -- 'VERIFIED', 'UNVERIFIED'
                                     contact_email VARCHAR(255),
                                     whatsapp_number VARCHAR(50),
                                     logo_url VARCHAR(255),
                                     banner_url VARCHAR(255),
                                     location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
                                     created_by BIGINT NOT NULL REFERENCES users(id),
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_honours (
                                            id BIGSERIAL PRIMARY KEY,
                                            club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                            title VARCHAR(255) NOT NULL,
                                            year_won INTEGER,
                                            description TEXT
);

CREATE TABLE IF NOT EXISTS club_opportunities (
                                                  id BIGSERIAL PRIMARY KEY,
                                                  club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                                  type VARCHAR(50) NOT NULL, -- 'FUNDRAISING', 'JOB', 'VOLUNTEER', 'WISHLIST'
                                                  title VARCHAR(255) NOT NULL,
                                                  description TEXT,
                                                  status VARCHAR(20) DEFAULT 'OPEN',
                                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_sponsors (
                                             id BIGSERIAL PRIMARY KEY,
                                             club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                             sponsor_name VARCHAR(255) NOT NULL,
                                             logo_url VARCHAR(255),
                                             website_url VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS club_memberships (
                                                club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                role VARCHAR(50) NOT NULL, -- 'OWNER', 'ADMIN', 'COACH'
                                                joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (club_id, user_id)
);

CREATE TABLE IF NOT EXISTS squads (
                                      id BIGSERIAL PRIMARY KEY,
                                      club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                      name VARCHAR(100) NOT NULL,
                                      category VARCHAR(50) NOT NULL, -- 'U18', 'FIRST_TEAM', etc.
                                      gender VARCHAR(20) DEFAULT 'MALE',
                                      head_coach_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS squad_players (
                                             squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             jersey_number INTEGER,
                                             squad_role VARCHAR(50) DEFAULT 'PLAYER', -- 'CAPTAIN', 'PLAYER', 'TRIALIST'
                                             joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (squad_id, user_id)
);

-- ==========================================
-- 6. MATCHES, TOURNAMENTS & SCHEDULING
-- ==========================================
CREATE TABLE IF NOT EXISTS tournaments (
                                           id BIGSERIAL PRIMARY KEY,
                                           name VARCHAR(255) NOT NULL,
                                           host_club_id BIGINT REFERENCES clubs(id) ON DELETE SET NULL,
                                           start_date TIMESTAMP,
                                           end_date TIMESTAMP,
                                           status VARCHAR(50) DEFAULT 'PLANNING',
                                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tournament_participants (
                                                       tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
                                                       club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                                       joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                       PRIMARY KEY (tournament_id, club_id)
);

CREATE TABLE IF NOT EXISTS matches (
                                       id BIGSERIAL PRIMARY KEY,
                                       home_club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                       away_club_id BIGINT REFERENCES clubs(id) ON DELETE CASCADE,
                                       match_type VARCHAR(50) NOT NULL,
                                       home_squad_id BIGINT REFERENCES squads(id) ON DELETE CASCADE,
                                       away_squad_id BIGINT REFERENCES squads(id) ON DELETE CASCADE,
                                       status VARCHAR(50) NOT NULL,
                                       scheduled_date TIMESTAMP,
                                       location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS club_schedules (
                                              id BIGSERIAL PRIMARY KEY,
                                              club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                              date DATE NOT NULL,
                                              status VARCHAR(50) NOT NULL, -- 'FREE', 'BUSY', 'TRAINING', 'MATCH_DAY'
                                              notes TEXT,
                                              UNIQUE(club_id, date)
);

CREATE TABLE IF NOT EXISTS tryouts (
                                       id BIGSERIAL PRIMARY KEY,
                                       club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                       title VARCHAR(255) NOT NULL,
                                       description TEXT,
                                       position VARCHAR(50),
                                       age_group VARCHAR(50),
                                       tryout_date TIMESTAMP NOT NULL,
                                       deadline TIMESTAMP,
                                       location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
                                       created_by BIGINT NOT NULL REFERENCES users(id),
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tryout_applications (
                                                   id BIGSERIAL PRIMARY KEY,
                                                   tryout_id BIGINT NOT NULL REFERENCES tryouts(id) ON DELETE CASCADE,
                                                   user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                   status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                                   message TEXT,
                                                   applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   reviewed_at TIMESTAMP,
                                                   reviewed_by BIGINT REFERENCES users(id)
);

-- ==========================================
-- 7. SOCIAL, CONTENT & SCOUTING FEED
-- ==========================================
CREATE TABLE IF NOT EXISTS follows (
                                       follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       following_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       PRIMARY KEY (follower_id, following_id)
);

CREATE TABLE IF NOT EXISTS club_follows (
                                            user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                            club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
                                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (user_id, club_id)
);

CREATE TABLE IF NOT EXISTS posts (
                                     id BIGSERIAL PRIMARY KEY,
                                     author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     club_id BIGINT REFERENCES clubs(id) ON DELETE CASCADE,
                                     content TEXT NOT NULL,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     is_public BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS post_media (
                                          post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                          media_id BIGINT NOT NULL REFERENCES media(id) ON DELETE CASCADE,
                                          display_order INTEGER DEFAULT 0,
                                          PRIMARY KEY (post_id, media_id)
);

CREATE TABLE IF NOT EXISTS likes (
                                     post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                     user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     PRIMARY KEY (post_id, user_id)
);

CREATE TABLE IF NOT EXISTS comments (
                                        id BIGSERIAL PRIMARY KEY,
                                        post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                        user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                        content TEXT NOT NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS post_tags (
                                         post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         PRIMARY KEY (post_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_post_tags_user_id ON post_tags(user_id);

CREATE TABLE IF NOT EXISTS post_seen (
                                         post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
                                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                         seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         PRIMARY KEY (post_id, user_id)
);

-- ==========================================
-- 8. COMMUNICATIONS & MESSAGING
-- ==========================================
CREATE TABLE IF NOT EXISTS conversations (
                                             id BIGSERIAL PRIMARY KEY,
                                             name VARCHAR(100),
                                             context_type VARCHAR(50), -- 'DIRECT', 'GROUP', 'MATCH_CHALLENGE', 'AGENT_PITCH'
                                             context_id BIGINT,        -- Links to matches.id or tryouts.id if applicable
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS conversation_participants (
                                                         conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                                         user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                                         joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                         PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
                                        id BIGSERIAL PRIMARY KEY,
                                        conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
                                        sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                        content TEXT NOT NULL,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);

CREATE TABLE IF NOT EXISTS message_reads (
                                             message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (message_id, user_id)
);

-- ==========================================
-- 9. NOTIFICATIONS
-- ==========================================
CREATE TABLE IF NOT EXISTS notifications (
                                             id BIGSERIAL PRIMARY KEY,
                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                             type VARCHAR(50) NOT NULL, -- 'MATCH_CHALLENGE', 'TRYOUT_UPDATE', 'NEW_MESSAGE'
                                             entity_type VARCHAR(30),
                                             entity_id BIGINT,
                                             title VARCHAR(255),
                                             body TEXT,
                                             is_read BOOLEAN DEFAULT FALSE,
                                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

-- Default system injection
INSERT INTO public.conversations (id, name, context_type, created_at)
VALUES (1, 'Global Grassroots', 'GROUP', NOW())
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- 10. ROW LEVEL SECURITY (RLS)
-- ==========================================
-- Posts: Zero-Trust Policy
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

CREATE POLICY posts_tenant_isolation ON posts
    FOR ALL
    USING (author_id = NULLIF(current_setting('talanti.current_user_id', true), '')::BIGINT);

-- User Profiles: Public read/insert, private update
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY profiles_public_read ON user_profiles FOR SELECT USING (true);
CREATE POLICY profiles_public_insert ON user_profiles FOR INSERT WITH CHECK (true);

CREATE POLICY profiles_private_update ON user_profiles
    FOR UPDATE
    USING (user_id = NULLIF(current_setting('talanti.current_user_id', true), '')::BIGINT);

-- Player Details: Public read/insert, private update
ALTER TABLE player_details ENABLE ROW LEVEL SECURITY;

CREATE POLICY player_details_public_read ON player_details FOR SELECT USING (true);
CREATE POLICY player_details_public_insert ON player_details FOR INSERT WITH CHECK (true);

CREATE POLICY player_details_private_update ON player_details
    FOR UPDATE
    USING (user_id = NULLIF(current_setting('talanti.current_user_id', true), '')::BIGINT);

-- Follows: Users can only follow/unfollow on their own behalf
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;

CREATE POLICY follows_public_read ON follows FOR SELECT USING (true);

CREATE POLICY follows_private_write ON follows
    FOR ALL
    USING (follower_id = NULLIF(current_setting('talanti.current_user_id', true), '')::BIGINT);

ALTER TABLE users ADD CONSTRAINT check_user_type
    CHECK (user_type IN ('FAN', 'PLAYER', 'AGENT', 'CLUB_ADMIN', 'SYSTEM_ADMIN'));

ALTER TABLE matches
    ADD CONSTRAINT check_match_target
        CHECK (
            (home_club_id IS NOT NULL AND away_club_id IS NOT NULL) OR
            (home_squad_id IS NOT NULL AND away_squad_id IS NOT NULL)
            );

ALTER TABLE club_opportunities
    ADD COLUMN external_link VARCHAR(500);


ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS notifications_owner_only ON notifications;
CREATE POLICY notifications_owner_only
    ON notifications
    USING (user_id = NULLIF(current_setting('talanti.current_user_id', true), '')::BIGINT);

DROP POLICY IF EXISTS notifications_system_insert ON notifications;
CREATE POLICY notifications_system_insert
    ON notifications
    FOR INSERT
    WITH CHECK (true);


ALTER TABLE notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications FORCE ROW LEVEL SECURITY;


ALTER TABLE users
    ALTER COLUMN user_type SET DEFAULT 'PLAYER';

UPDATE user_profiles
SET full_name = NULL
WHERE full_name = 'New User';

CREATE INDEX IF NOT EXISTS idx_clubs_location_id ON clubs(location_id);
CREATE INDEX IF NOT EXISTS idx_tryouts_location_date ON tryouts(location_id, tryout_date);
CREATE INDEX IF NOT EXISTS idx_matches_match_type_status_date ON matches(match_type, status, scheduled_date);
CREATE INDEX IF NOT EXISTS idx_matches_location_id ON matches(location_id);
