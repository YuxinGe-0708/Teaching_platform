SOURCE /docker-entrypoint-initdb.d/core-schema.src;
SOURCE /docker-entrypoint-initdb.d/core-test-data.src;
CREATE DATABASE IF NOT EXISTS user_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS learning_service_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE user_db;
SOURCE /docker-entrypoint-initdb.d/user-schema.src;
USE learning_service_db;
SOURCE /docker-entrypoint-initdb.d/learning-schema.src;
