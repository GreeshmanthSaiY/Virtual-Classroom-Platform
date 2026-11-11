-- Flyway Migration V3: Security Hardening & Safe Purge of Demo Accounts
-- Safely removes exposed demo credentials and pre-seeded demonstration accounts.
-- Uses email subqueries to prevent accidental deletion of any legitimate non-demo records.

-- 1. Remove discussion answers and questions authored by demo accounts
DELETE FROM answers WHERE author_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

DELETE FROM questions WHERE author_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

-- 2. Remove demo study materials and video lectures
DELETE FROM study_materials WHERE uploader_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

DELETE FROM video_lectures WHERE faculty_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

-- 3. Remove demo faculty subject assignments
DELETE FROM faculty_subject_assignments WHERE faculty_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

-- 4. Remove profiles and notifications for demo accounts
DELETE FROM student_profiles WHERE user_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

DELETE FROM faculty_profiles WHERE user_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

DELETE FROM notifications WHERE user_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
);

-- 5. Clean audit logs for demo accounts
DELETE FROM audit_logs WHERE actor_id IN (
    SELECT id FROM users WHERE email IN (
        'admin@virtualclassroom.edu',
        'dr.jeevan@virtualclassroom.edu',
        'gaurav.shukla@virtualclassroom.edu',
        'alice.smith@virtualclassroom.edu',
        'prof.tushar@virtualclassroom.edu'
    )
) OR actor_email IN (
    'admin@virtualclassroom.edu',
    'dr.jeevan@virtualclassroom.edu',
    'gaurav.shukla@virtualclassroom.edu',
    'alice.smith@virtualclassroom.edu',
    'prof.tushar@virtualclassroom.edu'
);

-- 6. Delete demo users by known test emails
DELETE FROM users WHERE email IN (
    'admin@virtualclassroom.edu',
    'dr.jeevan@virtualclassroom.edu',
    'gaurav.shukla@virtualclassroom.edu',
    'alice.smith@virtualclassroom.edu',
    'prof.tushar@virtualclassroom.edu'
);

-- 7. Synchronize sequences
SELECT setval('users_id_seq', COALESCE((SELECT MAX(id) FROM users), 1), (SELECT COUNT(*) > 0 FROM users));
SELECT setval('questions_id_seq', COALESCE((SELECT MAX(id) FROM questions), 1), (SELECT COUNT(*) > 0 FROM questions));
