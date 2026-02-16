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