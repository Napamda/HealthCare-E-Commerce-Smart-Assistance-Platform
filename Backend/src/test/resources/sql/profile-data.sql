DELETE FROM professional_profiles;

INSERT INTO professional_profiles
    (id, user_id, first_name, last_name, title, specialty, bio, phone, address, city,
     latitude, longitude, active, created_at, updated_at)
VALUES
    (1, 1, 'Ama', 'Mensah', 'Dr', 'Cardiology', NULL, NULL, NULL, 'Accra',
     5.6037, -0.1870, TRUE, '2026-01-01 09:00:00', '2026-01-01 09:00:00'),
    (2, 2, 'Kofi', 'Owusu', 'Dr', 'General Practice', NULL, NULL, NULL, 'Accra',
     5.6140, -0.2000, TRUE, '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (3, 3, 'Esi', 'Appiah', 'Dr', 'Pediatrics', NULL, NULL, NULL, 'Accra',
     5.6140, -0.1870, TRUE, '2026-01-03 09:00:00', '2026-01-03 09:00:00'),
    (4, 4, 'Akua', 'Boateng', 'Dr', 'Cardiology', NULL, NULL, NULL, 'Accra',
     5.6887, -0.1020, TRUE, '2026-01-04 09:00:00', '2026-01-04 09:00:00'),
    (5, 5, 'Yaw', 'Darko', 'Dr', 'Cardiology', NULL, NULL, NULL, 'Accra',
     5.6037, -0.1870, FALSE, '2026-01-05 09:00:00', '2026-01-05 09:00:00'),
    (6, 6, 'Far', 'Dater', 'Dr', 'Cardiology', NULL, NULL, NULL, 'Kumasi',
     6.7000, -1.6300, TRUE, '2026-01-06 09:00:00', '2026-01-06 09:00:00');
