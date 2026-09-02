ARG MYSQL_IMAGE=mysql:8.0.40
FROM ${MYSQL_IMAGE}

COPY services/user-service/src/main/resources/db/schema-user.sql /docker-entrypoint-initdb.d/01-user.sql
COPY services/learning-service/src/main/resources/db/schema-learning.sql /docker-entrypoint-initdb.d/02-learning.sql
COPY services/assessment-service/src/main/resources/db/schema-assessment.sql /docker-entrypoint-initdb.d/03-assessment.sql
COPY services/user-service/src/main/resources/db/seed-user.sql /docker-entrypoint-initdb.d/11-seed-user.sql
COPY services/learning-service/src/main/resources/db/seed-learning.sql /docker-entrypoint-initdb.d/12-seed-learning.sql
COPY services/assessment-service/src/main/resources/db/seed-assessment.sql /docker-entrypoint-initdb.d/13-seed-assessment.sql
