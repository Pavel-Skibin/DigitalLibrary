-- ============================================
-- User Service Database Schema
-- Version: 1.0
-- Description: Initial schema for users and user roles
-- ============================================

-- Create user_roles table
CREATE TABLE user_roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Create users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role_id INTEGER NOT NULL,
    deleted_at TIMESTAMP,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES user_roles(id)
);

-- Indexes for users table
CREATE INDEX idx_user_username ON users(username);
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_user_role_id ON users(role_id);
CREATE INDEX idx_user_deleted_at ON users(deleted_at);

-- Insert default roles
INSERT INTO user_roles (id, name) VALUES 
    (1, 'USER'),
    (2, 'MODERATOR'),
    (3, 'ADMIN');

-- Set sequence for user_roles to start after inserted values
SELECT setval('user_roles_id_seq', (SELECT MAX(id) FROM user_roles));

-- Comments
COMMENT ON TABLE users IS 'User accounts for the digital library system';
COMMENT ON TABLE user_roles IS 'Available user roles (USER, MODERATOR, ADMIN)';
COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp';
