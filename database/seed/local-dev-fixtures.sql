-- ==============================================================================
-- Virtual Classroom Platform - Local Development Fixtures
-- WARNING: DO NOT USE IN PRODUCTION ENVIRONMENTS.
-- This script contains local testing fixtures and sample mock data for offline development.
-- ==============================================================================

-- 1. Insert Local Dev Administrator
-- Dev Password: 'DevAdmin@2026!'
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(1, 'dev.admin@virtualclassroom.local', '$2a$12$Z1iX7h5yVdEvl63r8qg.w.0y1N8gG83fT.FshF.4o34H.Q7K72bCe', 'Local System Admin', 'ROLE_ADMIN', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

-- 2. Insert Local Dev Faculty
-- Dev Password: 'DevFaculty@2026!'
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(2, 'dev.faculty@virtualclassroom.local', '$2a$12$Z1iX7h5yVdEvl63r8qg.w.0y1N8gG83fT.FshF.4o34H.Q7K72bCe', 'Dr. Dev Limaye', 'ROLE_FACULTY', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO faculty_profiles (user_id, employee_id, department) VALUES
(2, 'EMP-DEV-001', 'Department of Computer Science')
ON CONFLICT (user_id) DO NOTHING;

-- 3. Insert Local Dev Student
-- Dev Password: 'DevStudent@2026!'
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(3, 'dev.student@virtualclassroom.local', '$2a$12$Z1iX7h5yVdEvl63r8qg.w.0y1N8gG83fT.FshF.4o34H.Q7K72bCe', 'Dev Student Shukla', 'ROLE_STUDENT', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO student_profiles (user_id, roll_number, course_id, current_year) VALUES
(3, 'CB.EN.U4DEV21170', (SELECT id FROM courses WHERE code = 'BSC-CS'), 3)
ON CONFLICT (user_id) DO NOTHING;

-- 4. Assign Dev Faculty to Subjects
INSERT INTO faculty_subject_assignments (faculty_id, subject_id, academic_term) VALUES
(2, (SELECT id FROM subjects WHERE code = 'CS201'), '2026-SPRING'),
(2, (SELECT id FROM subjects WHERE code = 'CS301'), '2026-SPRING')
ON CONFLICT DO NOTHING;

-- Synchronize sequences
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
