package ryerson.ca.deletemovie.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class User_Movies_CRUD {
    private static Connection getConnection() throws SQLException {
        try {
            System.out.println("Attempting to connect to database using DBConfig");
            Connection con = DBConfig.getCon();
            System.out.println("Database connection successful");
            return con;
        } catch (RuntimeException e) {
            System.out.println("❌ Database connection error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public static boolean deleteMovie(int movieId, String email) {
        System.out.println("\n==== DELETE MOVIE DEBUG ====");
        System.out.println("Request to delete movie: " + movieId + " by user with email: " + email);
        
        Connection con = null;
        PreparedStatement getUserId = null;
        PreparedStatement checkAssociation = null;
        PreparedStatement deleteUserMovie = null;
        ResultSet userIdResult = null;
        ResultSet associationResult = null;
        
        try {
            con = getConnection();
            if (con == null) {
                System.out.println("Failed to get database connection");
                return false;
            }
            
            // Step 1: Get the UserID for the given email
            String getUserIdSQL = "SELECT UserID FROM User WHERE Email = ?";
            System.out.println("Executing SQL: " + getUserIdSQL + " with email=" + email);
            getUserId = con.prepareStatement(getUserIdSQL);
            getUserId.setString(1, email);
            userIdResult = getUserId.executeQuery();
            
            if (!userIdResult.next()) {
                System.out.println("No user found with email: " + email);
                return false;
            }
            
            int userId = userIdResult.getInt("UserID");
            System.out.println("UserID for email " + email + " is " + userId);
            
            // Step 2: Check if the user is associated with the movie
            String checkAssociationSQL = "SELECT * FROM User_ADD_Movie WHERE UserID = ? AND MovieID = ?";
            System.out.println("Executing SQL: " + checkAssociationSQL + " with UserID=" + userId + ", MovieID=" + movieId);
            checkAssociation = con.prepareStatement(checkAssociationSQL);
            checkAssociation.setInt(1, userId);
            checkAssociation.setInt(2, movieId);
            associationResult = checkAssociation.executeQuery();
            
            if (!associationResult.next()) {
                System.out.println("No association found between user and movie. Deletion not allowed.");
                return false;
            }
            
            System.out.println("Association check passed. Proceeding with deletion.");
            
            // Step 3: Delete the user-movie association
            String deleteUserMovieSQL = "DELETE FROM User_ADD_Movie WHERE UserID = ? AND MovieID = ?";
            System.out.println("Executing SQL: " + deleteUserMovieSQL + " with UserID=" + userId + ", MovieID=" + movieId);
            deleteUserMovie = con.prepareStatement(deleteUserMovieSQL);
            deleteUserMovie.setInt(1, userId);
            deleteUserMovie.setInt(2, movieId);
            int rowsDeleted = deleteUserMovie.executeUpdate();
            
            System.out.println("Rows deleted from User_ADD_Movie: " + rowsDeleted);
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("❌ SQL error during movie deletion: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (userIdResult != null) userIdResult.close();
                if (associationResult != null) associationResult.close();
                if (getUserId != null) getUserId.close();
                if (checkAssociation != null) checkAssociation.close();
                if (deleteUserMovie != null) deleteUserMovie.close();
                if (con != null) con.close();
                System.out.println("Resources closed");
                System.out.println("===========================\n");
            } catch (SQLException e) {
                System.out.println("❌ Error closing resources: " + e.getMessage());
            }
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
            e.printStackTrace();
            return false;
        }
    }
}
