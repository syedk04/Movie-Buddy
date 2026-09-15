package ryerson.ca.deletemovie.test;

import ryerson.ca.deletemovie.business.DeleteMovieService;

public class TestDeleteMovie {
    public static void main(String[] args) {
        System.out.println("Testing DeleteMovie functionality...");
        
        // Test case 1: Delete a movie that exists and user has permission
        testDeleteMovie(1, "user@example.com", "Movie exists and user has permission");
        
        // Test case 2: Delete a movie that exists but user doesn't have permission
        testDeleteMovie(2, "user@example.com", "Movie exists but user doesn't have permission");
        
        // Test case 3: Delete a movie that doesn't exist
        testDeleteMovie(999, "user@example.com", "Movie doesn't exist");
        
        // Test case 4: Delete a movie with invalid movie ID
        testDeleteMovie(-1, "user@example.com", "Invalid movie ID");
        
        // Test case 5: Delete a movie with null email
        testDeleteMovie(1, null, "Null email");
        
        // Test case 6: Delete a movie with empty email
        testDeleteMovie(1, "", "Empty email");
    }
    
    private static void testDeleteMovie(int movieId, String email, String testDescription) {
        System.out.println("\n=== Test: " + testDescription + " ===");
        System.out.println("Movie ID: " + movieId);
        System.out.println("Email: " + email);
        
        try {
            boolean result = DeleteMovieService.deleteMovie(movieId, email);
            System.out.println("Result: " + (result ? "Success" : "Failure"));
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("=== End of Test ===\n");
    }
}