CREATE UNIQUE INDEX ux_sectors_name_normalized ON sectors (LOWER(BTRIM(name)));
CREATE UNIQUE INDEX ux_users_email_normalized ON users (LOWER(BTRIM(email)));
