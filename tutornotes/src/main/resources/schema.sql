-- ============================================================
--  TutorNotes AI  —  MySQL Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS tutornotes;
USE tutornotes;

-- ------------------------------------------------------------
-- 1. Standards (state / grade / topic lookup table)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS standards (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    state       VARCHAR(50)  NOT NULL,
    grade       VARCHAR(50)  NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    description TEXT         NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_standard (state, grade, code)
);

-- ------------------------------------------------------------
-- 2. Students
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- 3. Session Notes  (core table)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS session_notes (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_type       ENUM('STUDENT','GROUP') NOT NULL DEFAULT 'STUDENT',
    student_names   VARCHAR(300) NOT NULL  COMMENT 'Comma-separated for group sessions',
    session_date    DATE         NOT NULL,
    standard_id     BIGINT       NULL,
    state           VARCHAR(10)  NULL,
    grade           VARCHAR(20)  NULL,
    topic           VARCHAR(100) NULL,
    engagement      TEXT         NOT NULL  COMMENT 'Tutor observation: engagement & behaviour',
    skills          TEXT         NOT NULL  COMMENT 'Tutor observation: skills & specific moments',
    activities      VARCHAR(500) NULL      COMMENT 'Tools/activities used (IXL, Blooket, etc.)',
    generated_note  TEXT         NULL      COMMENT 'AI-generated session note',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_standard FOREIGN KEY (standard_id) REFERENCES standards(id) ON DELETE SET NULL
);

-- ------------------------------------------------------------
-- 4. Seed: Standards data
-- ------------------------------------------------------------
INSERT IGNORE INTO standards (state, grade, topic, description) VALUES
-- Common Core
('CC','III','Multiplication & Division','Multiplication facts, arrays, properties, intro to division'),
('CC','IV','Multiplication & Division','Multi-digit multiplication, long division with remainders'),
('CC','V','Decimals & Percentages','Place value in decimals, comparing, adding/subtracting decimals'),
('CC','V','Fractions','Equivalent fractions, adding/subtracting unlike denominators'),
('CC','VI','Fractions','Dividing fractions, fraction word problems'),
('CC','VI','Decimals & Percentages','Converting fractions to decimals, percentages of quantities'),
('CC','VII','Algebraic Thinking','Expressions, equations, inequalities, proportional reasoning'),
-- Florida
('FL','III','Multiplication & Division','MAFS.3.OA: multiplication and division within 100'),
('FL','IV','Multiplication & Division','MAFS.4.NBT: multi-digit multiplication and division'),
('FL','V','Decimals & Percentages','MAFS.5.NBT: decimal place value, operations with decimals'),
('FL','VI','Fractions','MAFS.6.NS: dividing fractions, fraction operations'),
-- Texas
('TX','IV','Multiplication & Division','TEKS 4.4: multi-digit multiplication, division with remainders'),
('TX','V','Decimals & Percentages','TEKS 5.3: decimal operations and place value'),
('TX','VI','Fractions','TEKS 6.3: fraction operations and applications');

-- ------------------------------------------------------------
-- Indexes for common queries
-- ------------------------------------------------------------
CREATE INDEX idx_notes_date        ON session_notes(session_date);
CREATE INDEX idx_notes_type        ON session_notes(note_type);
CREATE INDEX idx_notes_student     ON session_notes(student_names(50));
CREATE INDEX idx_standards_lookup  ON standards(state, grade);
