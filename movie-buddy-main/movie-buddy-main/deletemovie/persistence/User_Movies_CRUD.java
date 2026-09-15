package ryerson.ca.deletemovie.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class User_Movies_CRUD {

    private static final Logger logger = Logger.getLogger(User_Movies_CRUD.class.getName());

    public static boolean deleteMovie(int movieId, String email) {
        logger.info("Delete request: movieId=" + movieId + ", email=" + email);

        String getUserIdSQL         = "SELECT UserID FROM User WHERE Email = ?";
        String checkAssociationSQL  = "SELECT 1 FROM User_ADD_Movie WHERE UserID = ? AND MovieID = ?";
        String deleteUserMovieSQL   = "DELETE FROM User_ADD_Movie WHERE UserID = ? AND MovieID = ?";

        try (Connection con = DBConfig.getCon()) {
            if (con == null) {
                logger.severe("Database connection is null");
                return false;
            }

            int userId;
            try (PreparedStatement ps = con.prepareStatement(getUserIdSQL)) {
                ps.setString(1, email);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    logger.warning("No user found with email: " + email);
                    return false;
                }
                userId = rs.getInt("UserID");
            }

            try (PreparedStatement ps = con.prepareStatement(checkAssociationSQL)) {
                ps.setInt(1, userId);
                ps.setInt(2, movieId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    logger.warning("User " + email + " does not own movie " + movieId);
                    return false;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(deleteUserMovieSQL)) {
                ps.setInt(1, userId);
                ps.setInt(2, movieId);
                boolean deleted = ps.executeUpdate() > 0;
                logger.info("Movie " + movieId + " deletion " + (deleted ? "succeeded" : "failed"));
                return deleted;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "SQL error deleting movie " + movieId, e);
            return false;
        }
    }

    public static boolean linkMovieToUser(String email, String title, String director) {
        String sql = "INSERT INTO User_ADD_Movie (UserID, MovieID) " +
                     "SELECT u.UserID, m.MovieID FROM User u, Movie m " +
                     "WHERE u.Email = ? AND m.Title = ? AND m.Director = ?";

        try (Connection con = DBConfig.getCon();
             PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, title);
            stmt.setString(3, director);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error linking movie to user: " + email, e);
            return false;
        }
    }
}
