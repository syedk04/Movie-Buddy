package ryerson.ca.addservice.Persistence;

import ryerson.ca.addservice.Helper.AddedMovies;
import ryerson.ca.addservice.Helper.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class User_Movies_CRUD {

    private static final Logger logger = Logger.getLogger(User_Movies_CRUD.class.getName());

    public static Set<AddedMovies> searchForMovies(String email) {
        return searchMoviesWithFilters(email, null, null, null);
    }

    /**
     * Searches for movies belonging to a user with optional title/genre/director filters.
     * Pass null or empty string for any filter to skip it.
     */
    public static Set<AddedMovies> searchMoviesWithFilters(
            String email, String titleFilter, String genreFilter, String directorFilter) {

        Set<AddedMovies> movies = new HashSet<>();

        StringBuilder sql = new StringBuilder(
            "SELECT M.MovieID, M.Title, M.Genre, M.Director, M.Rating, U.UserID, U.Email " +
            "FROM Movie M " +
            "JOIN User_ADD_Movie UAM ON M.MovieID = UAM.MovieID " +
            "JOIN User U ON UAM.UserID = U.UserID " +
            "WHERE U.Email = ?");

        List<String> params = new ArrayList<>();
        params.add(email);

        if (titleFilter != null && !titleFilter.trim().isEmpty()) {
            sql.append(" AND M.Title LIKE ?");
            params.add("%" + titleFilter.trim() + "%");
        }
        if (genreFilter != null && !genreFilter.trim().isEmpty()) {
            sql.append(" AND M.Genre LIKE ?");
            params.add("%" + genreFilter.trim() + "%");
        }
        if (directorFilter != null && !directorFilter.trim().isEmpty()) {
            sql.append(" AND M.Director LIKE ?");
            params.add("%" + directorFilter.trim() + "%");
        }

        try (Connection con = DBConfig.getCon();
             PreparedStatement stmt = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                AddedMovies movie = new AddedMovies(
                    rs.getInt("MovieID"),
                    rs.getString("Title"),
                    rs.getString("Genre"),
                    rs.getString("Director")
                );
                movie.setRating(rs.getInt("Rating"));

                if (rs.getString("Email") != null) {
                    User user = new User(rs.getInt("UserID"), rs.getString("Email"));
                    movie.addUsers(Collections.singletonList(user));
                }
                movies.add(movie);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error searching movies for email: " + email, e);
            throw new RuntimeException("Error searching movies: " + e.getMessage(), e);
        }
        return movies;
    }

    /**
     * Adds a new movie and associates it with the given user's email.
     */
    public static boolean addMovie(String title, String genre, String director, String email) {
        String findUserQuery    = "SELECT UserID FROM User WHERE Email = ?";
        String createUserQuery  = "INSERT INTO User (Email) VALUES (?)";
        String insertMovieQuery = "INSERT INTO Movie (Title, Genre, Director, Rating) VALUES (?, ?, ?, 0)";
        String insertLinkQuery  = "INSERT INTO User_ADD_Movie (UserID, MovieID) VALUES (?, ?)";

        try (Connection con = DBConfig.getCon()) {
            if (con == null) {
                throw new RuntimeException("Database connection is null");
            }

            int userId;
            try (PreparedStatement psUser = con.prepareStatement(findUserQuery)) {
                psUser.setString(1, email);
                ResultSet userRs = psUser.executeQuery();
                if (userRs.next()) {
                    userId = userRs.getInt("UserID");
                    logger.fine("Found existing user with ID: " + userId);
                } else {
                    try (PreparedStatement psCreate = con.prepareStatement(
                            createUserQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        psCreate.setString(1, email);
                        psCreate.executeUpdate();
                        ResultSet newKeys = psCreate.getGeneratedKeys();
                        if (!newKeys.next()) {
                            throw new RuntimeException("Failed to create new user");
                        }
                        userId = newKeys.getInt(1);
                        logger.fine("Created new user with ID: " + userId);
                    }
                }
            }

            int movieId;
            try (PreparedStatement psMovie = con.prepareStatement(
                    insertMovieQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
                psMovie.setString(1, title);
                psMovie.setString(2, genre);
                psMovie.setString(3, director);
                psMovie.executeUpdate();
                ResultSet movieKeys = psMovie.getGeneratedKeys();
                if (!movieKeys.next()) {
                    throw new RuntimeException("Failed to retrieve generated MovieID");
                }
                movieId = movieKeys.getInt(1);
                logger.fine("Inserted movie with ID: " + movieId);
            }

            try (PreparedStatement psLink = con.prepareStatement(insertLinkQuery)) {
                psLink.setInt(1, userId);
                psLink.setInt(2, movieId);
                psLink.executeUpdate();
            }

            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error adding movie: " + title, e);
            throw new RuntimeException("Error adding movie: " + e.getMessage(), e);
        }
    }

    /**
     * Updates a movie's title, genre, and director.
     * Only succeeds if the requesting user owns the movie.
     */
    public static boolean updateMovie(int movieId, String title, String genre, String director, String email) {
        String verifyOwnershipSql =
            "SELECT UAM.MovieID FROM User_ADD_Movie UAM " +
            "JOIN User U ON UAM.UserID = U.UserID " +
            "WHERE UAM.MovieID = ? AND U.Email = ?";
        String updateSql = "UPDATE Movie SET Title = ?, Genre = ?, Director = ? WHERE MovieID = ?";

        try (Connection con = DBConfig.getCon()) {
            if (con == null) return false;

            try (PreparedStatement psCheck = con.prepareStatement(verifyOwnershipSql)) {
                psCheck.setInt(1, movieId);
                psCheck.setString(2, email);
                ResultSet rs = psCheck.executeQuery();
                if (!rs.next()) {
                    logger.warning("Update denied: user " + email + " does not own movie " + movieId);
                    return false;
                }
            }

            try (PreparedStatement psUpdate = con.prepareStatement(updateSql)) {
                psUpdate.setString(1, title);
                psUpdate.setString(2, genre);
                psUpdate.setString(3, director);
                psUpdate.setInt(4, movieId);
                return psUpdate.executeUpdate() > 0;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating movie " + movieId, e);
            return false;
        }
    }

    /**
     * Rates a movie (1-5). Only succeeds if the requesting user owns the movie.
     */
    public static boolean rateMovie(int movieId, int rating, String email) {
        if (rating < 1 || rating > 5) {
            logger.warning("Invalid rating value: " + rating);
            return false;
        }

        String verifyOwnershipSql =
            "SELECT UAM.MovieID FROM User_ADD_Movie UAM " +
            "JOIN User U ON UAM.UserID = U.UserID " +
            "WHERE UAM.MovieID = ? AND U.Email = ?";
        String rateSql = "UPDATE Movie SET Rating = ? WHERE MovieID = ?";

        try (Connection con = DBConfig.getCon()) {
            if (con == null) return false;

            try (PreparedStatement psCheck = con.prepareStatement(verifyOwnershipSql)) {
                psCheck.setInt(1, movieId);
                psCheck.setString(2, email);
                ResultSet rs = psCheck.executeQuery();
                if (!rs.next()) {
                    logger.warning("Rate denied: user " + email + " does not own movie " + movieId);
                    return false;
                }
            }

            try (PreparedStatement psRate = con.prepareStatement(rateSql)) {
                psRate.setInt(1, rating);
                psRate.setInt(2, movieId);
                return psRate.executeUpdate() > 0;
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error rating movie " + movieId, e);
            return false;
        }
    }
}
