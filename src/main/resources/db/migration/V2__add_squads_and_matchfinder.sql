-- ==============================================================================
-- 1. SQUADS (The actual teams within a club)
-- Connects directly to your existing 'clubs' and 'users' (for coaches) tables
-- ==============================================================================
CREATE TABLE IF NOT EXISTS squads (
                                      id BIGSERIAL PRIMARY KEY,
                                      club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,             -- e.g., "Dinamo U16 Boys"
    category VARCHAR(50) NOT NULL,          -- e.g., "U16", "FIRST_TEAM", "RESERVES"
    gender VARCHAR(20) DEFAULT 'MALE',      -- 'MALE', 'FEMALE', 'MIXED'
    head_coach_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ==============================================================================
-- 2. SQUAD PLAYERS (The specific roster for a squad)
-- Extends your existing user_profiles by mapping players directly to a squad
-- ==============================================================================
CREATE TABLE IF NOT EXISTS squad_players (
                                             squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    jersey_number INTEGER,
    squad_role VARCHAR(50) DEFAULT 'PLAYER', -- 'KEY_PLAYER', 'RESERVE', 'CAPTAIN'
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (squad_id, user_id)
    );

-- ==============================================================================
-- 3. MATCH REQUESTS (The MatchFinder feature)
-- Uses your polymorphic 'locations' table so it can show up directly on the map
-- ==============================================================================
CREATE TABLE IF NOT EXISTS match_requests (
                                              id BIGSERIAL PRIMARY KEY,
                                              club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- Strict access control audit
    desired_date TIMESTAMP NOT NULL,
    location_pref VARCHAR(50) NOT NULL,      -- 'CAN_HOST', 'WILL_TRAVEL', 'NEUTRAL'
    location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL, -- Links to existing locations table
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- 'OPEN', 'MATCHED', 'CANCELLED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ==============================================================================
-- 4. MATCHES (The actual scheduled games)
-- Handles matches against other squads on Talanti, or off-platform teams
-- ==============================================================================
CREATE TABLE IF NOT EXISTS matches (
                                       id BIGSERIAL PRIMARY KEY,
                                       home_squad_id BIGINT NOT NULL REFERENCES squads(id) ON DELETE CASCADE,
    away_squad_id BIGINT REFERENCES squads(id) ON DELETE SET NULL, -- Null if playing a non-Talanti team
    away_team_name VARCHAR(100),             -- Used if away_squad_id is null
    kickoff_time TIMESTAMP NOT NULL,
    location_id BIGINT REFERENCES locations(id) ON DELETE SET NULL,
    home_score INTEGER,
    away_score INTEGER,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELLED'
    match_type VARCHAR(50) DEFAULT 'FRIENDLY',       -- 'LEAGUE', 'CUP', 'FRIENDLY'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

-- ==============================================================================
-- 5. CAREER HISTORY (Stats tied to seasons, solving the generic stats problem)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS career_history (
                                              id BIGSERIAL PRIMARY KEY,
                                              user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    club_name VARCHAR(100) NOT NULL, -- String, so they can list past clubs not on the app
    season VARCHAR(20) NOT NULL,     -- e.g., "2024/2025"
    category VARCHAR(50),            -- e.g., "U18"
    appearances INTEGER DEFAULT 0,
    goals INTEGER DEFAULT 0,
    assists INTEGER DEFAULT 0,
    clean_sheets INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

-- ==============================================================================
-- 6. ALTER USER PROFILES (Adding the Free Agent / Marketplace fields)
-- ==============================================================================
ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS availability_status VARCHAR(50) DEFAULT 'IN_CLUB', -- 'FREE_AGENT', 'OPEN_TO_OFFERS', 'TRIALING'
    ADD COLUMN IF NOT EXISTS height_cm INTEGER,
    ADD COLUMN IF NOT EXISTS weight_kg INTEGER;

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_match_requests_status ON match_requests(status);
CREATE INDEX IF NOT EXISTS idx_matches_kickoff ON matches(kickoff_time);
CREATE INDEX IF NOT EXISTS idx_squad_players_user ON squad_players(user_id);