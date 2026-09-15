package ryerson.ca.addservice.Business;

import ryerson.ca.addservice.Persistence.User_Movies_CRUD;
import ryerson.ca.addservice.Helper.AddedMovies;
import ryerson.ca.addservice.Helper.AddedMoviesXML;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AddBusiness {

    private static final Logger logger = Logger.getLogger(AddBusiness.class.getName());

    public AddedMoviesXML getMoviesByUser(String email) {
        return buildMoviesXML(User_Movies_CRUD.searchForMovies(email));
    }

    /**
     * Fetches movies for a user with optional filters on title, genre, and director.
     * Passing null or empty string for a filter skips that filter.
     */
    public AddedMoviesXML searchMoviesWithFilters(
            String email, String titleFilter, String genreFilter, String directorFilter) {
        Set<AddedMovies> movies = User_Movies_CRUD.searchMoviesWithFilters(
                email, titleFilter, genreFilter, directorFilter);
        return buildMoviesXML(movies);
    }

    public boolean addMovie(String title, String genre, String director, String email) {
        boolean success = User_Movies_CRUD.addMovie(title, genre, director, email);
        if (success) {
            try {
                String message = "MOVIE_ADDED:" + email + ":" + title + ":" + genre + ":" + director;
                Messaging.sendmessage(message);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to publish MOVIE_ADDED event for: " + title, e);
            }
        }
        return success;
    }

    /**
     * Updates a movie's title, genre, and director.
     * Returns false if the user does not own the movie.
     */
    public boolean updateMovie(int movieId, String title, String genre, String director, String email) {
        return User_Movies_CRUD.updateMovie(movieId, title, genre, director, email);
    }

    /**
     * Rates a movie from 1 to 5 stars.
     * Returns false if the user does not own the movie or the rating is out of range.
     */
    public boolean rateMovie(int movieId, int rating, String email) {
        return User_Movies_CRUD.rateMovie(movieId, rating, email);
    }

    private AddedMoviesXML buildMoviesXML(Set<AddedMovies> movies) {
        Map<String, AddedMovies> byTitle = new HashMap<>();
        for (AddedMovies movie : movies) {
            if (byTitle.containsKey(movie.getTitle())) {
                byTitle.get(movie.getTitle()).addUsers(movie.getUsers());
            } else {
                byTitle.put(movie.getTitle(), movie);
            }
        }
        logger.fine("Returning " + byTitle.size() + " unique movie(s)");
        AddedMoviesXML moviesXML = new AddedMoviesXML();
        moviesXML.setMovies(new ArrayList<>(byTitle.values()));
        return moviesXML;
    }
}
