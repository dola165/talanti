CREATE TABLE IF NOT EXISTS users (

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

CREATE INDEX IF NOT EXISTS idx_users_role ON users(system_role);



-- 1. Conversations (Groups or Private chats)

CREATE TABLE IF NOT EXISTS conversations (

                                             id BIGSERIAL PRIMARY KEY,

                                             name VARCHAR(100), -- Optional: for named group chats

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

    );



-- 2. Participants (Who is in which conversation)

CREATE TABLE IF NOT EXISTS conversation_participants (

                                                         conversation_id BIGINT NOT NULL REFERENCES conversations(id),

    user_id BIGINT NOT NULL REFERENCES users(id),

    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (conversation_id, user_id)

    );



-- 3. Messages (The actual chats)

CREATE TABLE IF NOT EXISTS messages (

                                        id BIGSERIAL PRIMARY KEY,

                                        conversation_id BIGINT NOT NULL REFERENCES conversations(id),

    sender_id BIGINT NOT NULL REFERENCES users(id),

    content TEXT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

    );



-- Indexes for performance

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON messages(conversation_id);



CREATE TABLE IF NOT EXISTS user_profiles (

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

CREATE INDEX IF NOT EXISTS idx_profiles_name ON user_profiles(full_name);







CREATE TABLE IF NOT EXISTS message_reads (

                                             message_id BIGINT NOT NULL REFERENCES messages(id) ON DELETE CASCADE,

    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    read_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (message_id, user_id)

    );



-- 7. MEDIA (Files)

CREATE TABLE IF NOT EXISTS media (

                                     id BIGSERIAL PRIMARY KEY,

                                     url TEXT NOT NULL,

                                     type VARCHAR(50),

    size_bytes BIGINT,

    uploaded_by BIGINT REFERENCES users(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

    );



-- 8. NOTIFICATIONS

CREATE TABLE IF NOT EXISTS notifications (

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



CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);





-- Security & Auth

CREATE TABLE IF NOT EXISTS oauth2_logins (

                                             user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    provider VARCHAR(50) NOT NULL,

    provider_id VARCHAR(255) NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (user_id, provider)

    );



-- Core Entities

CREATE TABLE IF NOT EXISTS locations (

                                         id BIGSERIAL PRIMARY KEY,

                                         name VARCHAR(255) NOT NULL,

    address VARCHAR(255),

    latitude DECIMAL(10, 8) NOT NULL,

    longitude DECIMAL(11, 8) NOT NULL,

    type VARCHAR(20)

    );



CREATE TABLE IF NOT EXISTS clubs (

                                     id BIGSERIAL PRIMARY KEY,

                                     name VARCHAR(255) NOT NULL,

    description TEXT,

    location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,

    type VARCHAR(20) NOT NULL,

    logo_url VARCHAR(255),

    banner_url VARCHAR(255),

    is_official BOOLEAN DEFAULT FALSE,

    created_by BIGINT NOT NULL REFERENCES users(id),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP

    );



-- Social Graph

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



CREATE TABLE IF NOT EXISTS club_memberships (

                                                club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,

    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    role VARCHAR(50) NOT NULL,

    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (club_id, user_id)

    );



-- Content & Feed

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



CREATE TABLE IF NOT EXISTS post_seen (

                                         post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,

    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    seen_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (post_id, user_id)

    );



-- Tryouts

CREATE TABLE IF NOT EXISTS tryouts (

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



-- 1. Clear out any existing fake locations so we don't hit NOT NULL constraint errors

TRUNCATE TABLE locations CASCADE;



-- 2. Drop the 'name' column (we will get the name by joining the clubs/users table via entity_id)

ALTER TABLE locations DROP COLUMN IF EXISTS name;



-- 3. Rename and modify the 'type' column to 'entity_type'

-- If 'type' column exists, rename it. In V4 it wasn't there yet, so we'll just add entity_type if it doesn't exist.

-- But since this is a clean migrate from V4, let's just make it robust.

DO $$

BEGIN

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='type') THEN

ALTER TABLE locations RENAME COLUMN type TO entity_type;

ELSE

            IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='entity_type') THEN

ALTER TABLE locations ADD COLUMN entity_type VARCHAR(50);

END IF;

END IF;

END $$;



ALTER TABLE locations ALTER COLUMN entity_type TYPE VARCHAR(50);

ALTER TABLE locations ALTER COLUMN entity_type SET NOT NULL;



-- 4. Rename 'address' to 'address_text' to match our JOOQ repository

DO $$

BEGIN

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='locations' AND column_name='address') THEN

ALTER TABLE locations RENAME COLUMN address TO address_text;

END IF;

END $$;



-- 5. Add the new mandatory columns for the polymorphic design

ALTER TABLE locations ADD COLUMN IF NOT EXISTS entity_id BIGINT NOT NULL;

ALTER TABLE locations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();



-- 6. Add performance indexes for the map queries

CREATE INDEX IF NOT EXISTS idx_locations_entity ON locations(entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_locations_lat_lng ON locations(latitude, longitude);





INSERT INTO public.conversations (id, name, created_at)

VALUES (1, 'Global Grassroots', NOW())

    ON CONFLICT (id) DO NOTHING;


-- 1. Create the Post Tags table (The core of the scouting engine)
CREATE TABLE public.post_tags (
                                  post_id BIGINT NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
                                  user_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (post_id, user_id)
);

-- Index for faster lookups when loading a player's Match Feed
CREATE INDEX idx_post_tags_user_id ON public.post_tags(user_id);


-- 1. SQUADS (The actual teams within a club)
CREATE TABLE IF NOT EXISTS squads (
                                      id BIGSERIAL PRIMARY KEY,
                                      club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    gender VARCHAR(20) DEFAULT 'MALE',
    head_coach_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 2. SQUAD PLAYERS (The specific roster for a squad)
CREATE TABLE IF NOT EXISTS squad_players (
                                             squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jersey_number INTEGER,
    squad_role VARCHAR(50) DEFAULT 'PLAYER',
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (squad_id, user_id)
    );

-- 3. MATCH REQUESTS (The MatchFinder feature)
CREATE TABLE IF NOT EXISTS match_requests (
                                              id BIGSERIAL PRIMARY KEY,
                                              club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    desired_date TIMESTAMP NOT NULL,
    location_pref VARCHAR(50) NOT NULL,
    location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- 4. CAREER HISTORY (Stats tied to seasons)
CREATE TABLE IF NOT EXISTS career_history (
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

-- 5. ALTER USER PROFILES (Adding the Scouting fields)
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(50) DEFAULT 'IN_CLUB',
    ADD COLUMN IF NOT EXISTS height_cm INTEGER,
    ADD COLUMN IF NOT EXISTS weight_kg INTEGER;



CREATE TABLE IF NOT EXISTS club_events (
                                           id BIGSERIAL PRIMARY KEY,
                                           club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL, -- 'TRAINING', 'MEETING', etc.
    event_date TIMESTAMP NOT NULL,
    location_text VARCHAR(255),
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_club_events_club_date ON club_events(club_id, event_date);



-- 1. JWT Token Tracking (Allows us to revoke compromised sessions or force logout)
CREATE TABLE IF NOT EXISTS auth_tokens (
                                           id BIGSERIAL PRIMARY KEY,
                                           jti VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE INDEX idx_auth_tokens_jti ON auth_tokens(jti);
CREATE INDEX idx_auth_tokens_user ON auth_tokens(user_id);

-- 2. Password Reset Tokens (For the "Forgot Password" flow)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id BIGSERIAL PRIMARY KEY,
                                                     token VARCHAR(128) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);