-- Flyway Migration V2: Seed Academic Curriculum, Demo Accounts & Verification Data

-- 1. Degrees
INSERT INTO degrees (code, name, duration_years) VALUES
('B.Sc', 'Bachelor of Science', 3),
('M.Sc', 'Master of Science', 2),
('B.Tech', 'Bachelor of Technology', 4),
('B.A.', 'Bachelor of Arts', 3),
('M.A.', 'Master of Arts', 2)
ON CONFLICT (code) DO NOTHING;

-- 2. Courses
INSERT INTO courses (degree_id, code, name) VALUES
((SELECT id FROM degrees WHERE code = 'B.Sc'), 'BSC-CS', 'Computer Science'),
((SELECT id FROM degrees WHERE code = 'B.Sc'), 'BSC-DS', 'Data Science'),
((SELECT id FROM degrees WHERE code = 'B.Sc'), 'BSC-CA', 'Computer Application'),
((SELECT id FROM degrees WHERE code = 'M.Sc'), 'MSC-CS', 'Computer Science'),
((SELECT id FROM degrees WHERE code = 'M.Sc'), 'MSC-CA', 'Computer Application'),
((SELECT id FROM degrees WHERE code = 'B.A.'), 'BA-ENG', 'English Literature'),
((SELECT id FROM degrees WHERE code = 'B.A.'), 'BA-PSY', 'Psychology'),
((SELECT id FROM degrees WHERE code = 'M.A.'), 'MA-ENG', 'English Literature'),
((SELECT id FROM degrees WHERE code = 'M.A.'), 'MA-PSY', 'Psychology')
ON CONFLICT (code) DO NOTHING;

-- 3. Core Subjects
INSERT INTO subjects (course_id, code, title, year_level) VALUES
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS101', 'Python Programming', 1),
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS102', 'Data Structures & Algorithms', 1),
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS201', 'Object-Oriented Programming with Java', 2),
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS202', 'Database Management Systems', 2),
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS301', 'Software Engineering & Agile Methodologies', 3),
((SELECT id FROM courses WHERE code = 'BSC-CS'), 'CS302', 'Computer Networks & Security', 3),
((SELECT id FROM courses WHERE code = 'BSC-DS'), 'DS101', 'Applied Statistics for Data Science', 1),
((SELECT id FROM courses WHERE code = 'BSC-DS'), 'DS201', 'Machine Learning Foundations', 2)
ON CONFLICT (code) DO NOTHING;

-- 4. Initial Users
-- BCrypt hash for 'Admin@2026!' is '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy'
-- BCrypt hash for 'Teacher@2026!' is '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy'
-- BCrypt hash for 'Student@2026!' is '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy'

-- System Administrator
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(1, 'admin@virtualclassroom.edu', '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy', 'System Administrator', 'ROLE_ADMIN', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

-- Verified Professor (Faculty)
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(2, 'dr.jeevan@virtualclassroom.edu', '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy', 'Dr. Jeevan Limaye', 'ROLE_FACULTY', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO faculty_profiles (user_id, employee_id, department) VALUES
(2, 'EMP-CS-001', 'Department of Computer Science')
ON CONFLICT (user_id) DO NOTHING;

-- Verified Student
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(3, 'gaurav.shukla@virtualclassroom.edu', '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy', 'Gaurav Shukla', 'ROLE_STUDENT', 'APPROVED')
ON CONFLICT (email) DO NOTHING;

INSERT INTO student_profiles (user_id, roll_number, course_id, current_year) VALUES
(3, 'CB.EN.U4CSE21170', (SELECT id FROM courses WHERE code = 'BSC-CS'), 3)
ON CONFLICT (user_id) DO NOTHING;

-- Pending Student Applicant for Admin Approval verification
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(4, 'alice.smith@virtualclassroom.edu', '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy', 'Alice Smith', 'ROLE_STUDENT', 'PENDING')
ON CONFLICT (email) DO NOTHING;

INSERT INTO student_profiles (user_id, roll_number, course_id, current_year) VALUES
(4, 'CB.EN.U4CSE21201', (SELECT id FROM courses WHERE code = 'BSC-CS'), 1)
ON CONFLICT (user_id) DO NOTHING;

-- Pending Faculty Applicant for Admin Approval verification
INSERT INTO users (id, email, password_hash, full_name, role, status) VALUES
(5, 'prof.tushar@virtualclassroom.edu', '$2a$10$w81.65tKsmwXg.iS/v6mueP6UaY4s/R0UoN.1/576Vw8mK4s.4sXy', 'Prof. Tushar Deshmukh', 'ROLE_FACULTY', 'PENDING')
ON CONFLICT (email) DO NOTHING;

INSERT INTO faculty_profiles (user_id, employee_id, department) VALUES
(5, 'EMP-CS-002', 'Department of Data Science')
ON CONFLICT (user_id) DO NOTHING;

-- Faculty Subject Assignment
INSERT INTO faculty_subject_assignments (faculty_id, subject_id, academic_term) VALUES
(2, (SELECT id FROM subjects WHERE code = 'CS201'), '2026-SPRING'),
(2, (SELECT id FROM subjects WHERE code = 'CS301'), '2026-SPRING')
ON CONFLICT DO NOTHING;

-- 5. Seed Study Materials
INSERT INTO study_materials (subject_id, uploader_id, title, description, file_url, file_size_bytes, mime_type) VALUES
((SELECT id FROM subjects WHERE code = 'CS201'), 2, 'Java Concurrency & Multithreading Guide', 'Comprehensive chapter notes on Java ExecutorService, Locks, and Virtual Threads.', 'https://storage.virtualclassroom.edu/materials/java-concurrency.pdf', 2450000, 'application/pdf'),
((SELECT id FROM subjects WHERE code = 'CS301'), 2, 'Agile & Scrum Architectural Case Studies', 'Sprint execution, user stories breakdown, and clean architecture principles.', 'https://storage.virtualclassroom.edu/materials/agile-architecture.pdf', 3150000, 'application/pdf')
ON CONFLICT DO NOTHING;

-- 6. Seed Video Lectures
INSERT INTO video_lectures (subject_id, faculty_id, title, description, video_url, duration_seconds) VALUES
((SELECT id FROM subjects WHERE code = 'CS201'), 2, 'Lecture 01: JVM Memory Model & Garbage Collection', 'Deep dive into Heap, Metaspace, and generational garbage collection.', 'https://www.youtube.com/watch?v=uM_m6EzMg3k', 2850),
((SELECT id FROM subjects WHERE code = 'CS301'), 2, 'Lecture 01: Enterprise Layered Architecture vs Spaghetti Code', 'Refactoring legacy JSP systems into resilient Spring Boot micro-services.', 'https://www.youtube.com/watch?v=uM_m6EzMg3k', 3200)
ON CONFLICT DO NOTHING;

-- 7. Seed Q&A Forum Threads
INSERT INTO questions (id, subject_id, author_id, title, body, is_resolved) VALUES
(1, (SELECT id FROM subjects WHERE code = 'CS201'), 3, 'Difference between Synchronized Block and ReentrantLock?', 'Can someone clarify when ReentrantLock should be preferred over standard synchronized blocks in high-concurrency applications?', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO answers (question_id, author_id, body, is_accepted) VALUES
(1, 2, 'ReentrantLock provides features like tryLock() with timeout, fair locking policies, and multiple condition variables, which synchronized cannot provide.', TRUE)
ON CONFLICT DO NOTHING;

-- Synchronize sequences
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('questions_id_seq', (SELECT MAX(id) FROM questions));
