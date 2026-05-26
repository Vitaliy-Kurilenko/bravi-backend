ALTER TABLE users
DROP
COLUMN phone;

CREATE INDEX idx_userentity_ext_id ON users (ext_id);