package ryerson.ca.deletemovie.test;

import ryerson.ca.deletemovie.persistence.DBConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseCheck {
    public static void main(String[] args) {
        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            System.out.println("Attempting to connect to database...");
            con = DBConfig.getCon();
            System.out.println("Connection successful!");
            
            // Check if MOVIES table exists and has data
            System.out.println("\nChecking MOVIES table:");
            stmt = con.prepareStatement("SELECT * FROM MOVIES");
            rs = stmt.executeQuery();
            
            boolean hasMovies = false;
            while (rs.next()) {
                hasMovies = true;
                System.out.println("Movie ID: " + rs.getInt("movie_id") + 
                                  ", Title: " + rs.getString("title"));
            }
            
            if (!hasMovies) {
                System.out.println("No movies found in the database!");
            }
            
            // Check if USER_MOVIES table exists and has data
            System.out.println("\nChecking USER_MOVIES table:");
            stmt = con.prepareStatement("SELECT * FROM USER_MOVIES");
            rs = stmt.executeQuery();
            
            boolean hasUserMovies = false;
            while (rs.next()) {
                hasUserMovies = true;
                System.out.println("Email: " + rs.getString("email") + 
                                  ", Movie ID: " + rs.getInt("movie_id") + 
                                  ", Can Delete: " + rs.getBoolean("can_delete"));
            }
            
            if (!hasUserMovies) {
                System.out.println("No user-movie relationships found in the database!");
            }
            
        } catch (Exception e) {
            System.out.println("Error checking database: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (con != null) con.close();
            } catch (SQLException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }
    }
}