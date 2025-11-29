-- Database schema for MyDrive application 

-- Drop database if exists
DROP DATABASE IF EXISTS mydrive_db;
CREATE DATABASE mydrive_db;
USE mydrive_db;

-- Users table
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    storage_used BIGINT DEFAULT 0,
    storage_limit BIGINT DEFAULT 5368709120, -- 5GB default
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Directories table
CREATE TABLE directories (
    directory_id INT AUTO_INCREMENT PRIMARY KEY,
    directory_name VARCHAR(255) NOT NULL,
    owner_id INT NOT NULL,
    parent_directory_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (parent_directory_id) REFERENCES directories(directory_id) ON DELETE CASCADE
);

-- Files table
CREATE TABLE files (
    file_id INT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    owner_id INT NOT NULL,
    directory_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (directory_id) REFERENCES directories(directory_id) ON DELETE SET NULL
);

-- Shared access table for both files and directories
CREATE TABLE shared_items (
    share_id INT AUTO_INCREMENT PRIMARY KEY,
    item_type ENUM('file', 'directory') NOT NULL,
    item_id INT NOT NULL,
    owner_id INT NOT NULL,
    shared_with_id INT NOT NULL,
    permission_level ENUM('view', 'edit') DEFAULT 'view',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (shared_with_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- Add indexes for better performance
CREATE INDEX idx_directories_parent ON directories(parent_directory_id);
CREATE INDEX idx_files_directory ON files(directory_id);
CREATE INDEX idx_files_owner ON files(owner_id);
CREATE INDEX idx_directories_owner ON directories(owner_id);
CREATE INDEX idx_shared_items_shared_with ON shared_items(shared_with_id);

-- Insert a test admin user (password: admin123)
INSERT INTO users (username, password, email, full_name, storage_limit) 
VALUES ('admin', '$2a$10$xJwW9PHx8.k1ixjNgQzFZu6.L.iLXw8oK.ZZyLWrN3Gg9dvHHWgDK', 'admin@mydrive.com', 'System Admin', 10737418240); -- 10GB

-- Create root directory for admin
INSERT INTO directories (directory_name, owner_id, parent_directory_id)
VALUES ('Root', 1, NULL);