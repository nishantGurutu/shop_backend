CREATE TABLE IF NOT EXISTS farmer_users (
    id UUID PRIMARY KEY,
    user_id VARCHAR(30) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(16), village VARCHAR(100), district VARCHAR(100),
    state VARCHAR(100), language VARCHAR(40), land_area VARCHAR(40),
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_farmer_user_id UNIQUE (user_id)
);
CREATE TABLE IF NOT EXISTS farmer_crops (
    farmer_id UUID NOT NULL REFERENCES farmer_users(id),
    crop VARCHAR(80)
);
