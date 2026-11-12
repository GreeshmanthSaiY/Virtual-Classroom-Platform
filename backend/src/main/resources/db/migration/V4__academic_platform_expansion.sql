-- Flyway Migration V4: Complete Academic Platform Expansion
-- Introduces comprehensive schemas for:
-- 1. Identity & Profiles: avatar, phone, bio, designation, and refresh token rotation
-- 2. Academic Curriculum: subject semesters, credits, and student subject enrollments
-- 3. Live Virtual Classroom: scheduled sessions and meeting metadata
-- 4. Coursework & Assessments: assignments, student submissions, and grading
-- 5. Examinations & Grading: exams and official student evaluation results

-- =========================================================================
-- 1. Identity & Profile Schema Enhancements
-- =========================================================================

ALTER TABLE users 
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(30);

ALTER TABLE student_profiles 
    ADD COLUMN IF NOT EXISTS bio TEXT;

ALTER TABLE faculty_profiles 
    ADD COLUMN IF NOT EXISTS designation VARCHAR(100) DEFAULT 'Faculty Instructor',
    ADD COLUMN IF NOT EXISTS office_hours VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bio TEXT;

-- Refresh Token Rotation and Revocation Tracking
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_lookup ON refresh_tokens(token_hash, revoked, expires_at);

-- =========================================================================
-- 2. Academic Curriculum Enhancements & Student Subject Enrollment
-- =========================================================================

ALTER TABLE subjects 
    ADD COLUMN IF NOT EXISTS semester INT DEFAULT 1 CHECK (semester BETWEEN 1 AND 10),
    ADD COLUMN IF NOT EXISTS credits INT DEFAULT 3 CHECK (credits BETWEEN 1 AND 10),
    ADD COLUMN IF NOT EXISTS description TEXT;

-- Student Subject Enrollment (Explicit Course/Subject Registration per Term)
CREATE TABLE IF NOT EXISTS student_subject_enrollments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    academic_term VARCHAR(30) NOT NULL DEFAULT '2026-ACTIVE',
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED' CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')),
    enrolled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_student_subject_term UNIQUE (student_id, subject_id, academic_term)
);

CREATE INDEX IF NOT EXISTS idx_enrollments_student ON student_subject_enrollments(student_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_subject ON student_subject_enrollments(subject_id);

-- Backfill enrollment for any active students matching course and year level
INSERT INTO student_subject_enrollments (student_id, subject_id, academic_term, status)
SELECT sp.user_id, s.id, '2026-ACTIVE', 'ENROLLED'
FROM student_profiles sp
JOIN subjects s ON s.course_id = sp.course_id AND s.year_level = sp.current_year
ON CONFLICT (student_id, subject_id, academic_term) DO NOTHING;

-- =========================================================================
-- 3. Live Virtual Classroom Sessions
-- =========================================================================

CREATE TABLE IF NOT EXISTS live_sessions (
    id BIGSERIAL PRIMARY KEY,
    subject_id INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    host_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    meeting_link VARCHAR(500) NOT NULL,
    platform VARCHAR(50) NOT NULL DEFAULT 'GOOGLE_MEET',
    scheduled_start TIMESTAMP WITH TIME ZONE NOT NULL,
    scheduled_end TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED' CHECK (status IN ('SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_live_sessions_subject ON live_sessions(subject_id, status);
CREATE INDEX IF NOT EXISTS idx_live_sessions_schedule ON live_sessions(scheduled_start);

-- =========================================================================
-- 4. Coursework, Assignments & Student Submissions
-- =========================================================================

CREATE TABLE IF NOT EXISTS assignments (
    id BIGSERIAL PRIMARY KEY,
    subject_id INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    faculty_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    max_points INT NOT NULL DEFAULT 100 CHECK (max_points > 0),
    attachment_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_assignments_subject_due ON assignments(subject_id, due_date);
CREATE INDEX IF NOT EXISTS idx_assignments_faculty ON assignments(faculty_id);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id BIGSERIAL PRIMARY KEY,
    assignment_id BIGINT NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submission_text TEXT,
    file_url VARCHAR(500),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    grade_points INT CHECK (grade_points >= 0),
    feedback TEXT,
    graded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    graded_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'LATE', 'GRADED', 'RESUBMISSION_REQUESTED')),
    CONSTRAINT uk_assignment_student UNIQUE (assignment_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_submissions_assignment ON assignment_submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student ON assignment_submissions(student_id);

-- =========================================================================
-- 5. Formal Examinations & Academic Results
-- =========================================================================

CREATE TABLE IF NOT EXISTS examinations (
    id BIGSERIAL PRIMARY KEY,
    subject_id INT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    creator_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(200) NOT NULL,
    exam_type VARCHAR(30) NOT NULL DEFAULT 'MIDTERM' CHECK (exam_type IN ('QUIZ', 'MIDTERM', 'FINAL', 'ASSESSMENT')),
    total_marks INT NOT NULL DEFAULT 100 CHECK (total_marks > 0),
    duration_minutes INT NOT NULL DEFAULT 60 CHECK (duration_minutes > 0),
    scheduled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    instructions TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_examinations_subject ON examinations(subject_id);

CREATE TABLE IF NOT EXISTS examination_results (
    id BIGSERIAL PRIMARY KEY,
    examination_id BIGINT NOT NULL REFERENCES examinations(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    marks_obtained NUMERIC(5,2) NOT NULL CHECK (marks_obtained >= 0),
    grade VARCHAR(10) NOT NULL,
    remarks TEXT,
    graded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    graded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_exam_student UNIQUE (examination_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_exam_results_exam ON examination_results(examination_id);
CREATE INDEX IF NOT EXISTS idx_exam_results_student ON examination_results(student_id);

