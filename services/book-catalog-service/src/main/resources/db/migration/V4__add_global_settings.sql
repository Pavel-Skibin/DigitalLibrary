CREATE TABLE IF NOT EXISTS global_settings (
    key   VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL
);

-- По умолчанию читалка отключена
INSERT INTO global_settings (key, value)
VALUES ('reading_enabled', 'false')
ON CONFLICT (key) DO NOTHING;
