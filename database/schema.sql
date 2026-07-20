

CREATE DATABASE IF NOT EXISTS disaster_relief_db;
USE disaster_relief_db;


CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    contact_number VARCHAR(20),
    role ENUM('ADMIN', 'RESCUE_TEAM', 'POLICE_STATION', 'VOLUNTEER', 'REPORTER') NOT NULL,

    --admin
    department VARCHAR(100),
    //rescueTeam
    unit_name VARCHAR(100),
    assigned_zone VARCHAR(100),
    //PoliceStation
    station_name VARCHAR(100),
    station_code VARCHAR(50),
    jurisdiction_area VARCHAR(100),
    // Volunteer
    available BOOLEAN,
    // Reporter
    relationship_to_missing_person VARCHAR(100),

    
    approved BOOLEAN DEFAULT TRUE
);

//disaster_events
CREATE TABLE disaster_events (
    disaster_id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('EARTHQUAKE', 'FLOOD', 'LANDSLIDE', 'CYCLONE', 'TSUNAMI', 'FIRE', 'OTHER') NOT NULL,
    description TEXT,
    date_occurred DATE NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255),
    radius_km DOUBLE NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    date_created DATE DEFAULT (CURRENT_DATE),
    created_by INT,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);

//person
CREATE TABLE persons (
    person_id INT AUTO_INCREMENT PRIMARY KEY,
    person_type ENUM('LOST', 'FOUND') NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    age INT,
    gender VARCHAR(20),
    physical_description TEXT,
    photo_url VARCHAR(500),
    status ENUM('MISSING', 'FOUND', 'DECEASED', 'UNIDENTIFIED') NOT NULL,
    //locations
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(255),

    //lostPerson
    circumstances TEXT,
    //foundPerson
    condition_at_discovery VARCHAR(100),

    date_reported DATE DEFAULT (CURRENT_DATE),
    date_last_updated DATE DEFAULT (CURRENT_DATE),

    disaster_id INT,
    reported_by INT,
    last_updated_by INT,

    FOREIGN KEY (disaster_id) REFERENCES disaster_events(disaster_id),
    FOREIGN KEY (reported_by) REFERENCES users(user_id),
    FOREIGN KEY (last_updated_by) REFERENCES users(user_id)
);

//match_records
CREATE TABLE match_records (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    missing_person_id INT NOT NULL,
    found_person_id INT NOT NULL,
    confidence_score DOUBLE NOT NULL,
    status ENUM('SUGGESTED', 'CONFIRMED', 'REJECTED') DEFAULT 'SUGGESTED',
    verified_by INT,
    verified_at DATETIME,

    FOREIGN KEY (missing_person_id) REFERENCES persons(person_id),
    FOREIGN KEY (found_person_id) REFERENCES persons(person_id),
    FOREIGN KEY (verified_by) REFERENCES users(user_id)
);

//--sample inputs
INSERT INTO users (full_name, username, password_hash, role, department)
VALUES ('System Admin', 'admin', 'admin123', 'ADMIN', 'IT Operations');

INSERT INTO users (full_name, username, password_hash, role, unit_name, assigned_zone)
VALUES ('Kasun Fernando', 'rescueteam1', 'rescue123', 'RESCUE_TEAM', 'Southern Rescue Unit', 'Ratnapura');

INSERT INTO users (full_name, username, password_hash, role, station_name, station_code, jurisdiction_area)
VALUES ('Ratnapura Police', 'police1', 'police123', 'POLICE_STATION', 'Ratnapura Police Station', 'RTN-01', 'Ratnapura District');

INSERT INTO users (full_name, username, password_hash, role, available)
VALUES ('Ishara Silva', 'volunteer1', 'volunteer123', 'VOLUNTEER', TRUE);

INSERT INTO users (full_name, username, password_hash, role, relationship_to_missing_person)
VALUES ('Sarah Perera', 'reporter1', 'reporter123', 'REPORTER', 'Daughter');

INSERT INTO disaster_events (type, description, date_occurred, latitude, longitude, address, radius_km, created_by)
VALUES ('FLOOD', 'Heavy flooding in Ratnapura district', '2026-06-01', 6.6828, 80.4014, 'Ratnapura, Sri Lanka', 15.0, 1);

INSERT INTO persons (person_type, full_name, age, gender, physical_description, photo_url, status, latitude, longitude, address, circumstances, disaster_id, reported_by)
VALUES ('LOST', 'Nimal Perera', 54, 'Male', 'Wearing a blue shirt, has a scar on left arm', NULL, 'MISSING', 6.6850, 80.4050, 'Near Ratnapura town', 'Last seen near the river before floodwaters rose', 1, 1);

INSERT INTO persons (person_type, full_name, age, gender, physical_description, photo_url, status, latitude, longitude, address, condition_at_discovery, disaster_id, reported_by)
VALUES ('FOUND', 'Unknown Male', 55, 'Male', 'Approx. 55 years, found injured', NULL, 'FOUND', 6.6790, 80.3990, 'Ratnapura relief center', 'Stable, minor injuries', 1, 1);
