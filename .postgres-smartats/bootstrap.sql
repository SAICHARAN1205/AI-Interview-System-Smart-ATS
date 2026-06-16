DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'smartats') THEN
        CREATE ROLE smartats LOGIN PASSWORD 'smartats';
    ELSE
        ALTER ROLE smartats WITH LOGIN PASSWORD 'smartats';
    END IF;
END
$$;
SELECT 'CREATE DATABASE smartats OWNER smartats'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'smartats')\gexec
GRANT ALL PRIVILEGES ON DATABASE smartats TO smartats;
