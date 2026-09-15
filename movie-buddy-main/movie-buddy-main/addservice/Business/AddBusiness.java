package ryerson.ca.addservice.Business;

import java.io.IOException;
import ryerson.ca.addservice.Persistence.User_Movies_CRUD;
import ryerson.ca.addservice.Helper.AddedMovies;
import ryerson.ca.addservice.Helper.AddedMoviesXML;
import ryerson.ca.addservice.Business.Messaging;
import java.util.*;

public class AddBusiness {

    /**
     * Fetches movies for a specific user and groups them by user.
     *
     * @param email The email of the user.
     * @return An AddedMoviesXML object containing the user's movies.
     */
    public AddedMoviesXML getMoviesByUser(String email) {
        Set<AddedMovies> movies = User_Movies_CRUD.searchForMovies(email);
        Map<String, AddedMovies> allUserMovies = new HashMap<>();

        System.out.println("✅ Number of movies found: " + movies.size());

        for (AddedMovies movie : movies) {
            if (allUserMovies.containsKey(movie.getTitle())) {
                allUserMovies.get(movie.getTitle()).addUsers(movie.getUsers());
            } else {
                allUserMovies.put(movie.getTitle(), movie);
            }
        }

        System.out.println("✅ Total Unique Movies: " + allUserMovies.size());

        AddedMoviesXML moviesXML = new AddedMoviesXML();
        moviesXML.setMovies(new ArrayList<>(allUserMovies.values()));

        return moviesXML;
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
    public boolean addMovie(String title, String genre, String director, String email) {
        boolean success = User_Movies_CRUD.addMovie(title, genre, director, email);
        if (success) {
            try {
                String message = "MOVIE_ADDED:" + email + ":" + title + ":" + genre + ":" + director;
                Messaging.sendmessage(message);
            } catch (IOException e) {
                System.err.println("Failed to publish event");
            }
        }
        return success;
    }
}