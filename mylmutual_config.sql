-- SQL script to insert the "mylmutual" app configuration
-- This uses the USER_POOL_ID and USER_POOL_CLIENT_ID provided in the issue description

-- First, check if the app already exists with this name
DO $$
DECLARE
    app_exists BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM app_config WHERE app_name = 'mylmutual'
    ) INTO app_exists;
    
    IF app_exists THEN
        -- Update existing app
        UPDATE app_config
        SET aws_region = 'eu-west-1',
            user_pool_id = 'eu-west-1_eugpZQ2pa',
            client_id = '67sh3qrs9kkekr23fj2li9ntlt',
            client_secret = NULL,
            enabled = true
        WHERE app_name = 'mylmutual';
    ELSE
        -- Insert new app
        INSERT INTO app_config (
            app_key,
            app_name,
            aws_region,
            user_pool_id,
            client_id,
            client_secret,
            enabled
        ) VALUES (
            gen_random_uuid(), -- Generate a random UUID
            'mylmutual',
            'eu-west-1',
            'eu-west-1_eugpZQ2pa',
            '67sh3qrs9kkekr23fj2li9ntlt',
            NULL, -- No client secret needed
            true
        );
    END IF;
END $$;

-- Display the configuration
SELECT * FROM app_config WHERE app_name = 'mylmutual';