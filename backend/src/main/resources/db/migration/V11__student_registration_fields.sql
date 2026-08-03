ALTER TABLE users ADD COLUMN IF NOT EXISTS student_index text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS course text;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_student_index
    ON users (student_index)
    WHERE student_index IS NOT NULL;
