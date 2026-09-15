# Movie Buddy

A Java-based microservices web application for managing a personal movie collection. Users can register, log in, and maintain their own list of movies with full CRUD support, search and filtering, and a rating system.

---

## Architecture

The project is split into three deployable services:

| Service | Context Path | Responsibility |
|---|---|---|
| **FrontEnd** | `/FrontEnd` | Servlet, authentication, UI routing |
| **AddService** | `/AddService` | Add, retrieve, update, search, and rate movies |
| **DeleteMovie** | `/DeleteMovie` | Delete movies; subscribes to KubeMQ events to sync state |

Services communicate over HTTP REST (JAX-RS). Events are published via KubeMQ so the delete service stays in sync when a movie is added.

---

## Features

- **User authentication** using JWT tokens stored as `HttpOnly` cookies
- **Add movies** with title, genre, and director
- **Edit movies** - update title, genre, or director (owner only)
- **Delete movies** (owner only)
- **Search and filter** the movie list by title, genre, or director
- **Rate movies** from 1 to 5 stars (owner only)
- **Connection pooling** via a proxy-based pool (no external dependencies required)
- **Password hashing** using SHA-256 with a random salt for new registrations

---

## Environment Variables

All sensitive configuration is loaded from environment variables. Set these before starting each service:

| Variable | Description | Example |
|---|---|---|
| `DB_URL` | MySQL host and port | `localhost:3306` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `yourpassword` |
| `JWT_SECRET` | Secret key for signing JWTs (min 32 characters) | `change-me-in-production-32chars!` |
| `addService` | Host:port of the AddService | `localhost:8080` |
| `deleteService` | Host:port of the DeleteMovie service | `localhost:8081` |
| `kubeMQAddress` | KubeMQ server address | `localhost:50000` |

---

## Database Setup

### FrontendDB (used by FrontEnd service)

```sql
CREATE DATABASE FrontendDB;
USE FrontendDB;

CREATE TABLE Users (
    UserID   INT AUTO_INCREMENT PRIMARY KEY,
    Username VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL,
    Email    VARCHAR(255) NOT NULL UNIQUE
);
```

### ADDMOVIE_DB (used by AddService)

```sql
CREATE DATABASE ADDMOVIE_DB;
USE ADDMOVIE_DB;

CREATE TABLE User (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    Email  VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE Movie (
    MovieID  INT AUTO_INCREMENT PRIMARY KEY,
    Title    VARCHAR(255) NOT NULL,
    Genre    VARCHAR(100),
    Director VARCHAR(255),
    Rating   INT DEFAULT 0
);

CREATE TABLE User_ADD_Movie (
    UserID  INT NOT NULL,
    MovieID INT NOT NULL,
    PRIMARY KEY (UserID, MovieID),
    FOREIGN KEY (UserID)  REFERENCES User(UserID),
    FOREIGN KEY (MovieID) REFERENCES Movie(MovieID)
);
```

> **Migration note:** If you already have an `ADDMOVIE_DB` without the `Rating` column, run:
> ```sql
> ALTER TABLE Movie ADD COLUMN Rating INT DEFAULT 0;
> ```

### Delete_Movie_DB (used by DeleteMovie service)

```sql
CREATE DATABASE Delete_Movie_DB;
USE Delete_Movie_DB;

CREATE TABLE User (
    UserID INT AUTO_INCREMENT PRIMARY KEY,
    Email  VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE Movie (
    MovieID  INT AUTO_INCREMENT PRIMARY KEY,
    Title    VARCHAR(255) NOT NULL,
    Genre    VARCHAR(100),
    Director VARCHAR(255)
);

CREATE TABLE User_ADD_Movie (
    UserID  INT NOT NULL,
    MovieID INT NOT NULL,
    PRIMARY KEY (UserID, MovieID),
    FOREIGN KEY (UserID)  REFERENCES User(UserID),
    FOREIGN KEY (MovieID) REFERENCES Movie(MovieID)
);
```

---

## REST API Reference

### AddService `/AddService/webresources/movies`

| Method | Path | Description |
|---|---|---|
| GET | `/user?email=` | Get all movies for a user |
| GET | `/search?email=&title=&genre=&director=` | Search/filter movies (all params optional except email) |
| POST | `/add` | Add a movie (`title`, `genre`, `director`, `email`) |
| POST | `/update/{movieId}` | Update a movie (`title`, `genre`, `director`, `email`) |
| POST | `/rate/{movieId}` | Rate a movie (`rating` 1-5, `email`) |

### DeleteMovie `/DeleteMovie/webresources/delete`

| Method | Path | Description |
|---|---|---|
| POST | `/{movieId}/{email}` | Delete a movie (ownership verified) |

---

## Frontend Actions

The servlet at `/FrontEnd` handles the following:

| Method | Action / URI | Description |
|---|---|---|
| GET | `?action=login` | Show login page |
| POST | `?action=login` | Submit credentials |
| GET | `?action=movies` | View movie list |
| GET | `?action=search&title=&genre=&director=` | Search movie list |
| POST | `/addmovie` | Add a movie |
| POST | `/deletemovie` | Delete a movie |
| POST | `/updatemovie` | Edit a movie |
| POST | `/ratemovie` | Rate a movie |

---

## Security Notes

- JWT tokens are signed with HS256. Set a strong `JWT_SECRET` in production (minimum 32 characters).
- Tokens expire after 24 hours and are stored in `HttpOnly` cookies to prevent JavaScript access.
- All database queries use prepared statements to prevent SQL injection.
- New user registrations store passwords hashed with SHA-256 and a random salt. Existing accounts with plaintext passwords continue to work via a legacy fallback until migrated.
- Enable HTTPS in production and set the `Secure` flag on the auth cookie (see `FrontEnd.java`).

---

## Running Locally

1. Start MySQL and create the three databases using the scripts above.
2. Deploy each service to your Java application server (e.g., GlassFish, Tomcat).
3. Set all required environment variables.
4. Open the app at `http://localhost:8080/FrontEnd?action=login`.
