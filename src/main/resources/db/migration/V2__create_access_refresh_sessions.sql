CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    farmer_id UUID NOT NULL REFERENCES farmer_users(id),
    access_token_hash VARCHAR(64) NOT NULL UNIQUE,
    refresh_token_hash VARCHAR(64) NOT NULL UNIQUE,
    access_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    refresh_expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by UUID
);
CREATE INDEX idx_auth_session_family ON auth_sessions(family_id);
CREATE INDEX idx_auth_session_refresh_expiry ON auth_sessions(refresh_expires_at);
CREATE INDEX idx_auth_session_farmer ON auth_sessions(farmer_id);
