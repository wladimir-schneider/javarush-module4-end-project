CREATE DATABASE IF NOT EXISTS world;
USE world;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS country_language;
DROP TABLE IF EXISTS city;
DROP TABLE IF EXISTS country;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE country (
                         id INT NOT NULL PRIMARY KEY,
                         code VARCHAR(3) NOT NULL,
                         code_2 VARCHAR(2) NOT NULL,
                         name VARCHAR(80) NOT NULL,
                         continent INT NOT NULL,
                         region VARCHAR(80) NOT NULL,
                         surface_area DECIMAL(12,2) NOT NULL,
                         indep_year SMALLINT NULL,
                         population INT NOT NULL,
                         life_expectancy DECIMAL(3,1) NULL,
                         gnp DECIMAL(12,2) NULL,
                         gnpo_id DECIMAL(12,2) NULL,
                         local_name VARCHAR(80) NULL,
                         government_form VARCHAR(80) NULL,
                         head_of_state VARCHAR(80) NULL,
                         capital INT NULL
);

CREATE TABLE city (
                      id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                      name VARCHAR(80) NOT NULL,
                      country_id INT NOT NULL,
                      district VARCHAR(80) NOT NULL,
                      population INT NOT NULL,
                      INDEX idx_city_country_id (country_id),
                      CONSTRAINT fk_city_country
                          FOREIGN KEY (country_id) REFERENCES country(id)
);

CREATE TABLE country_language (
                                  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                  country_id INT NOT NULL,
                                  language VARCHAR(80) NOT NULL,
                                  is_official BIT NOT NULL,
                                  percentage DECIMAL(4,1) NOT NULL,
                                  INDEX idx_country_language_country_id (country_id),
                                  CONSTRAINT fk_country_language_country
                                      FOREIGN KEY (country_id) REFERENCES country(id)
);

SET SESSION cte_max_recursion_depth = 5000;

INSERT INTO country (
    id,
    code,
    code_2,
    name,
    continent,
    region,
    surface_area,
    indep_year,
    population,
    life_expectancy,
    gnp,
    gnpo_id,
    local_name,
    government_form,
    head_of_state,
    capital
)
WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 239
)
SELECT
    i AS id,
    LPAD(i, 3, '0') AS code,
    CONCAT('D', MOD(i, 10)) AS code_2,
    CONCAT('Country ', i) AS name,
    MOD(i, 7) AS continent,
    CONCAT('Region ', MOD(i, 12)) AS region,
    1000.00 + i AS surface_area,
    1900 + MOD(i, 120) AS indep_year,
    1000000 + i * 1000 AS population,
    50.0 + MOD(i, 45) AS life_expectancy,
    10000.00 + i AS gnp,
    9000.00 + i AS gnpo_id,
    CONCAT('Local Country ', i) AS local_name,
    'Republic' AS government_form,
    CONCAT('Head ', i) AS head_of_state,
    NULL AS capital
FROM seq;

INSERT INTO country_language (
    country_id,
    language,
    is_official,
    percentage
)
WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 239
)
SELECT
    i AS country_id,
    'English' AS language,
    b'1' AS is_official,
    60.0 AS percentage
FROM seq;

INSERT INTO country_language (
    country_id,
    language,
    is_official,
    percentage
)
WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 239
)
SELECT
    i AS country_id,
    CONCAT('Language ', i) AS language,
    b'0' AS is_official,
    40.0 AS percentage
FROM seq;

INSERT INTO city (
    id,
    name,
    country_id,
    district,
    population
)
WITH RECURSIVE seq AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM seq WHERE i < 4079
)
SELECT
    i AS id,
    CONCAT('City ', i) AS name,
    MOD(i - 1, 239) + 1 AS country_id,
    CONCAT('District ', MOD(i, 50)) AS district,
    10000 + i * 10 AS population
FROM seq;

UPDATE country
SET capital = id
WHERE id BETWEEN 1 AND 239;

ALTER TABLE country
    ADD CONSTRAINT fk_country_capital
        FOREIGN KEY (capital) REFERENCES city(id);

SELECT 'country' AS table_name, COUNT(*) AS rows_count FROM country
UNION ALL
SELECT 'city' AS table_name, COUNT(*) AS rows_count FROM city
UNION ALL
SELECT 'country_language' AS table_name, COUNT(*) AS rows_count FROM country_language;