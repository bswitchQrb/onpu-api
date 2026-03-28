-- メール認証を廃止し、ログインID認証に変更
ALTER TABLE users ADD COLUMN login_id VARCHAR(50) NOT NULL DEFAULT '' AFTER id;
ALTER TABLE users ADD UNIQUE INDEX uk_login_id (login_id);
ALTER TABLE users DROP INDEX email;
ALTER TABLE users DROP COLUMN email;
ALTER TABLE users DROP COLUMN email_verified;
DROP TABLE email_verification_tokens;
