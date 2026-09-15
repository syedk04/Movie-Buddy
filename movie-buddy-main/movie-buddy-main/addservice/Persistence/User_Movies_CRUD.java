package ryerson.ca.addservice.Persistence;

import ryerson.ca.addservice.Helper.AddedMovies;
import ryerson.ca.addservice.Helper.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;

public class User_Movies_CRUD {

    /**
     * Searches for movies added by a specific user.
     *
     * @param email The email of the user.
     * @return A set of AddedMovies objects representing the user's movies.
     */
    public static Set<AddedMovies> searchForMovies(String email) {
        Set<AddedMovies> movies = new HashSet<>();
        String sql = "SELECT M.MovieID, M.Title, M.Genre, M.Director, U.UserID, U.Email " +
                     "FROM Movie M " +
                     "JOIN User_ADD_Movie UAM ON M.MovieID = UAM.MovieID " +
                     "JOIN User U ON UAM.UserID = U.UserID " +
                     "WHERE U.Email = ?";

        try (Connection con = DBConfig.getCon();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int movieID = rs.getInt("MovieID");
                String title = rs.getString("Title");
                String genre = rs.getString("Genre");
                String director = rs.getString("Director");

                AddedMovies movie = new AddedMovies(movieID, title, genre, director);

                if (rs.getString("Email") != null) {
                    User user = new User(rs.getInt("UserID"), rs.getString("Email"));
                    movie.addUsers(Collections.singletonList(user));  // Add as a single-element list
                }

                movies.add(movie);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Error searching movies: " + e.getMessage(), e);
        }
        return movies;
    }

    /**
     * Adds a new movie to the database and associates it with a user.
     *
     * @param title    The title of the movie.
     * @param genre    The genre of the movie.
     * @param director The director of the movie.
     * @param userId   The ID of the user.
     * @return true if the movie was added successfully, false otherwise.
     */
 /**
 * Adds a new movie to the database and associates it with a user.
 *
 * @param title    The title of the movie.
 * @param genre    The genre of the movie.
 * @param director The director of the movie.
 * @param email    The email of the user.
 * @return true if the movie was added successfully, false otherwise.
 */
public static boolean addMovie(String title, String genre, String director, String email) {
    String insertMovieQuery = "INSERT INTO Movie (Title, Genre, Director) VALUES (?, ?, ?)";
    String insertUserMovieQuery = "INSERT INTO User_ADD_Movie (UserID, MovieID) VALUES (?, ?)";
    String findUserQuery = "SELECT UserID FROM User WHERE Email = ?";
    String createUserQuery = "INSERT INTO User (Email) VALUES (?)";

    try (Connection con = DBConfig.getCon()) {
        if (con == null) {
            throw new RuntimeException("❌ Database connection is null.");
        }

        // First, find or create the user by email
        int userId;
        PreparedStatement psUser = con.prepareStatement(findUserQuery);
        psUser.setString(1, email);
        ResultSet userRs = psUser.executeQuery();
        
        if (userRs.next()) {
            // User exists, get their ID
            userId = userRs.getInt("UserID");
            System.out.println("Found existing user with ID: " + userId);
        } else {
            // User doesn't exist, create a new one
            PreparedStatement psCreateUser = con.prepareStatement(createUserQuery, PreparedStatement.RETURN_GENERATED_KEYS);
            psCreateUser.setString(1, email);
            psCreateUser.executeUpdate();
            
            ResultSet newUserRs = psCreateUser.getGeneratedKeys();
            if (newUserRs.next()) {
                userId = newUserRs.getInt(1);
                System.out.println("Created new user with ID: " + userId);
            } else {
                throw new RuntimeException("❌ Failed to create new user.");
            }
            psCreateUser.close();
        }
        psUser.close();

        // Now insert the movie
        PreparedStatement psMovie = con.prepareStatement(insertMovieQuery, PreparedStatement.RETURN_GENERATED_KEYS);
        psMovie.setString(1, title);
        psMovie.setString(2, genre);
        psMovie.setString(3, director);
        int rowsAffected = psMovie.executeUpdate();
        System.out.println("Rows affected in Movie table: " + rowsAffected);

        // Retrieve the auto-generated MovieID
        ResultSet rs = psMovie.getGeneratedKeys();
        if (rs == null) {
            throw new RuntimeException("❌ Failed to retrieve generated keys.");
        }
        int movieId = -1;
        if (rs.next()) {
            movieId = rs.getInt(1);
            System.out.println("Generated MovieID: " + movieId);
        }
        psMovie.close();

        if (movieId == -1) {
            throw new RuntimeException("❌ Failed to retrieve the generated MovieID.");
        }

        // Associate the movie with the user
        PreparedStatement psUserMovie = con.prepareStatement(insertUserMovieQuery);
        psUserMovie.setInt(1, userId);
        psUserMovie.setInt(2, movieId);
        rowsAffected = psUserMovie.executeUpdate();
        System.out.println("Rows affected in User_ADD_Movie table: " + rowsAffected);
        psUserMovie.close();

        return true;

    } catch (Exception e) {
        throw new RuntimeException("❌ Error adding movie: " + e.getMessage(), e);
    }
} }