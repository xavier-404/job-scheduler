-- Create the users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    address VARCHAR(500),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create an index on client_id for faster lookups
CREATE INDEX IF NOT EXISTS idx_client_id ON users(client_id);

-- Create the jobs table
CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    schedule_type VARCHAR(20) NOT NULL, -- IMMEDIATE, ONE_TIME, RECURRING
    cron_expression VARCHAR(100),
    time_zone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    start_time TIMESTAMP,
    next_fire_time TIMESTAMP,
    status VARCHAR(20) NOT NULL, -- SCHEDULED, RUNNING, COMPLETED_SUCCESS, COMPLETED_FAILURE
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create an index on client_id for the jobs table
CREATE INDEX IF NOT EXISTS idx_jobs_client_id ON jobs(client_id);

-- Create an index on status for the jobs table
CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);

-- Insert some sample data for testing
INSERT INTO users (id, client_id, name, email, address, phone)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'CLIENT_ABC', 'John Doe', 'john@example.com', '123 Main St', '555-123-4567'),
    ('22222222-2222-2222-2222-222222222222', 'CLIENT_ABC', 'Jane Smith', 'jane@example.com', '456 Elm St', '555-987-6543'),
    ('33333333-3333-3333-3333-333333333333', 'CLIENT_XYZ', 'Bob Johnson', 'bob@example.com', '789 Oak St', '555-456-7890'),
    ('44444444-4444-4444-4444-444444444444', 'CLIENT_XYZ', 'Alice Brown', 'alice@example.com', '101 Pine St', '555-111-2222'),
    ('55555555-5555-5555-5555-555555555555', 'CLIENT_123', 'Charlie Davis', 'charlie@example.com', '202 Maple Ave', '555-333-4444');