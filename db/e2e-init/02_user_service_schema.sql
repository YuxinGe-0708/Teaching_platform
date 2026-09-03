SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;
SOURCE /docker-entrypoint-initdb.d/shared/user-service-schema.sql;
