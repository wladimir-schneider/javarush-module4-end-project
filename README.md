# JavaRush Modul 4 Projekt: SQL, JDBC/Hibernate und Redis

Dieses Projekt setzt die Aufgabe aus der Vorlesungsdatei um: MySQL-Daten werden mit Hibernate gelesen, in ein Redis-DTO transformiert, in Redis gespeichert und anschließend wird die Lesezeit von Redis und MySQL verglichen.

## Inhalt

- Maven-Projekt mit Java 17
- Hibernate-Entities: `Country`, `City`, `CountryLanguage`
- DAO-Schicht: `CountryDAO`, `CityDAO`
- Redis-DTOs: `CityCountry`, `Language`
- P6Spy-Konfiguration zur Ausgabe der SQL-Queries
- Docker Compose für MySQL 8 und Redis Stack
- Demo-Dump unter `db/init/01-world-demo.sql`

## Start

```bash
docker compose up -d
mvn clean compile exec:java
```

RedisInsight ist nach dem Start unter Port `8001` erreichbar.

## Hinweise

Die Aufgabe im Kurs geht von einem `world`-Dump aus. Da der originale JavaRush-Dump nicht Teil der hochgeladenen Datei war, enthält dieses ZIP einen kompatiblen Demo-Dump mit 239 Ländern und 4079 Städten. Damit funktionieren die IDs aus der Aufgabe und das Performance-Szenario bleibt gleich. Falls du den offiziellen Kurs-Dump hast, kannst du `db/init/01-world-demo.sql` ersetzen oder die Datenbank über MySQL Workbench importieren.

## Einzelne Docker-Befehle aus der Aufgabe

MySQL:

```bash
docker run --name mysql -d -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root --restart unless-stopped -v mysql:/var/lib/mysql mysql:8
```

Redis Stack:

```bash
docker run -d --name redis-stack -p 6379:6379 -p 8001:8001 redis/redis-stack:latest
```

Redis ohne RedisInsight:

```bash
docker run -d --name redis -p 6379:6379 redis:latest
```
