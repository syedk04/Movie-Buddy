package ryerson.ca.deletemovie.business;

import ryerson.ca.deletemovie.persistence.User_Movies_CRUD;

public class DeleteMovieService {
    
    /**
     * Business logic for deleting a movie
     * 
     * @param movieId The ID of the movie to delete
     * @param email The email of the user requesting the deletion
     * @return true if movie was successfully deleted, false otherwise
     */
    public static boolean deleteMovie(int movieId, String email) {
        // Validate input parameters
        if (movieId <= 0 || email == null || email.trim().isEmpty()) {
            System.out.println("Invalid input parameters: movieId=" + movieId + ", email=" + email);
            return false;
        }
        
        System.out.println("DeleteMovieService: Processing deletion request for movieId=" + 
                           movieId + " by user " + email);
        
        // Call the persistence layer to delete the movie
        boolean result = User_Movies_CRUD.deleteMovie(movieId, email);
        
        System.out.println("DeleteMovieService: Deletion " + 
                          (result ? "successful" : "failed") + 
                          " for movieId=" + movieId);
        
        return result;
    }
}