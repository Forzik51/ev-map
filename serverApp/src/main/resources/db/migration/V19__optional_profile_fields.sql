ALTER TABLE app_user ALTER COLUMN phone TYPE varchar(16), ALTER COLUMN phone DROP NOT NULL, ALTER COLUMN birthdate DROP NOT NULL;
UPDATE app_user SET phone=NULL WHERE btrim(phone)='';
