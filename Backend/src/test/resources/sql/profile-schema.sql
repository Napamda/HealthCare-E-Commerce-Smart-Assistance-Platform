CREATE TABLE IF NOT EXISTS professional_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    title VARCHAR(100) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    bio VARCHAR(2000),
    phone VARCHAR(50),
    address VARCHAR(255),
    city VARCHAR(100),
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
