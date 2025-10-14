-- Create user_activity_log table
CREATE TABLE IF NOT EXISTS user_activity_log (
    id BIGSERIAL PRIMARY KEY,
    activity VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    user_pool_id VARCHAR(255) NOT NULL,
    app_name VARCHAR(255),
    status VARCHAR(20),
    error_message TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL
);

-- Create indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_user_activity_log_username ON user_activity_log(username);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_user_pool_id ON user_activity_log(user_pool_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_app_name ON user_activity_log(app_name);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_created_at ON user_activity_log(created_at);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_activity ON user_activity_log(activity);
CREATE INDEX IF NOT EXISTS idx_user_activity_log_status ON user_activity_log(status);

-- Add comments
COMMENT ON TABLE user_activity_log IS 'Stores user activity logs for tracing activities per user pool';
COMMENT ON COLUMN user_activity_log.activity IS 'Type of activity (LOGIN, REGISTER, VERIFY, etc.)';
COMMENT ON COLUMN user_activity_log.username IS 'Username/email of the user';
COMMENT ON COLUMN user_activity_log.user_pool_id IS 'AWS Cognito user pool ID';
COMMENT ON COLUMN user_activity_log.app_name IS 'Application name';
COMMENT ON COLUMN user_activity_log.status IS 'Status of the activity (SUCCESS, FAILURE)';
COMMENT ON COLUMN user_activity_log.error_message IS 'Error message if activity failed';
COMMENT ON COLUMN user_activity_log.ip_address IS 'IP address of the client';
COMMENT ON COLUMN user_activity_log.created_at IS 'Timestamp when the activity was logged';