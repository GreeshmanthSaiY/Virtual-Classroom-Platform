-- Flyway Migration V3: Security Hardening & Purge of Demo Accounts
-- Removes exposed demo credentials and pre-seeded demonstration accounts from production database.

-- 1. Remove discussion answers and questions authored by demo accounts
DELETE FROM answers WHERE author_id IN (1, 2, 3, 4, 5);
DELETE FROM questions WHERE author_id IN (1, 2, 3, 4, 5);

-- 2. Remove demo study materials and video lectures
DELETE FROM study_materials WHERE uploader_id IN (1, 2, 3, 4, 5);
DELETE FROM video_lectures WHERE faculty_id IN (1, 2, 3, 4, 5);

-- 3. Remove demo faculty subject assignments
DELETE FROM faculty_subject_assignments WHERE faculty_id IN (1, 2, 3, 4, 5);

-- 4. Remove profiles and audit logs for demo accounts
DELETE FROM student_profiles WHERE user_id IN (1, 2, 3, 4, 5);
DELETE FROM faculty_profiles WHERE user_id IN (1, 2, 3, 4, 5);
DELETE FROM audit_logs WHERE user_id IN (1, 2, 3, 4, 5);

-- 5. Delete demo users by known test emails
DELETE FROM users WHERE email IN (
    'admin@virtualclassroom.edu',
    'dr.jeevan@virtualclassroom.edu',
    'gaurav.shukla@virtualclassroom.edu',
    'alice.smith@virtualclassroom.edu',
    'prof.tushar@virtualclassroom.edu'
);

-- 6. Synchronize sequences
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), (SELECT COUNT(*) > 0 FROM users));
SELECT setval('questions_id_seq', COALESCE((SELECT MAX(id) FROM questions), 1), (SELECT COUNT(*) > 0 FROM questions));
