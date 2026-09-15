package ryerson.ca.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ryerson.ca.business.Business;
import ryerson.ca.helper.AddedMoviesXML;
import ryerson.ca.persistence.DBConfig;

@WebServlet(name = "FrontEnd", urlPatterns = {"/FrontEnd"})
public class FrontEnd extends HttpServlet {

    private final Authenticate auth = new Authenticate();
    private final String authenticationCookieName = "login_token";

    /**
     * Checks if the user is authenticated by validating the JWT token.
     *
     * @param request The HTTP request.
     * @return A Map.Entry containing the token and username if authenticated, or an empty entry if not.
     */
    private Entry<String, String> isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = "";

        // Extract the token from the cookies
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(authenticationCookieName)) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        // Verify the token
        if (!token.isEmpty()) {
            try {
                Entry<Boolean, String> verificationResult = auth.verify(token);
                if (verificationResult.getKey()) {
                    return new AbstractMap.SimpleEntry<>(token, verificationResult.getValue());
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace(); // Log the exception
            }
        }

        // Return an empty entry if the token is invalid or missing
        return new AbstractMap.SimpleEntry<>("", "");
    }

    /**
     * Processes requests for both HTTP GET and POST methods.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    // Call isAuthenticated once and store both values
    Entry<String, String> authResult = isAuthenticated(request);
    String token = authResult.getKey();
    String username = authResult.getValue();
    
    String action = request.getParameter("action");

    if (action == null) {
        // Default action: Show login page
        response.sendRedirect("login.jsp");
        return;
    }

    switch (action) {
        case "login":
            handleLogin(request, response);
            break;

        case "movies":
            handleMovies(request, response, token, username);
            break;

        default:
            // Invalid action: Show login page
            response.sendRedirect("login.jsp");
            break;
    }
}

    /**
     * Handles the login action.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
   private void handleLogin(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

    String username = request.getParameter("username");
    String password = request.getParameter("password");
    
    System.out.println("Login attempt for user: " + username);

    // Authenticate the user
    String token = Business.authenticate(username, password);
    System.out.println("Authentication result: " + (token != null ? "Success" : "Failed"));
    
    if (token != null) {
        // Create a cookie with the token
        Cookie newCookie = new Cookie(authenticationCookieName, token);
        response.addCookie(newCookie);
        System.out.println("Added cookie: " + authenticationCookieName + "=" + token);

        // Redirect to the movies action instead of directly to movies.jsp
        response.sendRedirect("FrontEnd?action=movies&email=" + username);
    } else {
        // Authentication failed: Show login page with an error message
        request.setAttribute("error", "Invalid username or password");
        RequestDispatcher dispatcher = request.getRequestDispatcher("login.jsp");
        dispatcher.forward(request, response);
    }
}

    /**
     * Handles the movies action.
     *
     * @param request The HTTP request.
     * @param response The HTTP response.
     * @param token The JWT token.
     * @param username The username.
     * @throws ServletException If a servlet-specific error occurs.
     * @throws IOException If an I/O error occurs.
     */
    private void handleMovies(HttpServletRequest request, HttpServletResponse response, String token, String username)
        throws ServletException, IOException {
    if (token.isEmpty()) {
        // User is not authenticated: Redirect to login page
        response.sendRedirect("login.jsp");
        return;
    }
    
    // Look up the user's email based on their username
    String email = getUserEmailFromDatabase(username);
    
    // Now use the correct email
    AddedMoviesXML moviesXML = Business.getMovies(token, email);
    
    
    request.setAttribute("username", username);
    request.setAttribute("movies", moviesXML);
    request.setAttribute("token", token);
    request.setAttribute("email", email);
    RequestDispatcher dispatcher = request.getRequestDispatcher("movies.jsp");
    dispatcher.forward(request, response);
}

    // New method to get user email
    private String getUserEmailFromDatabase(String username) {
        String email = null;
        String query = "SELECT Email FROM Users WHERE Username = ?";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                email = rs.getString("Email");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return email != null ? email : username; // Fallback to username if email not found
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

   @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.contains("/addmovie")) {
        handleAddMovie(request, response);
    } else if (uri.contains("/deletemovie")) {
        System.out.println("Handling delete movie request");
        handleDeleteMovie(request, response);
    } else {
        processRequest(request, response);
    }    
    }
    
    private void handleAddMovie(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // Get form parameters
        String title = request.getParameter("title");
        String genre = request.getParameter("genre");
        String director = request.getParameter("director");
        String email = request.getParameter("email");
        String token = request.getParameter("token");

        // If token is not in request, try to get from cookie
        if (token == null || token.isEmpty()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("login_token")) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // Validate input
        if (title == null || genre == null || director == null || email == null || token == null ||
            title.isEmpty() || genre.isEmpty() || director.isEmpty() || email.isEmpty() || token.isEmpty()) {
            request.setAttribute("errorMessage", "All fields are required");
            RequestDispatcher dispatcher = request.getRequestDispatcher("movies.jsp");
            dispatcher.forward(request, response);
            return;
        }

        // Call Business method to add the movie
        boolean success = Business.addMovie(token, title, genre, director, email);

       if (success) {
        request.setAttribute("successMessage", "Movie added successfully!");
    } else {
        request.setAttribute("errorMessage", "Failed to add movie.");
    }

        // Re-set auth data before forwarding
        request.setAttribute("token", token);           // Preserve token
        request.setAttribute("email", email);           // Preserve email

        // Reload movies
        AddedMoviesXML moviesXML = Business.getMovies(token, email);
        request.setAttribute("movies", moviesXML);

        RequestDispatcher dispatcher = request.getRequestDispatcher("movies.jsp");
        dispatcher.forward(request, response);
        }
    
    private void handleDeleteMovie(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    String movieId = request.getParameter("movieId");
    String email = request.getParameter("email");
    String token = request.getParameter("token");

    // If token is not in request, try to get it from cookies
    if (token == null || token.isEmpty()) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("login_token")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
    }

    if (movieId == null || email == null || token == null || 
        movieId.isEmpty() || email.isEmpty() || token.isEmpty()) {
        request.setAttribute("errorMessage", "Invalid delete request");
        RequestDispatcher dispatcher = request.getRequestDispatcher("movies.jsp");
        dispatcher.forward(request, response);
        return;
    }

    boolean success = Business.deleteMovie(token, movieId, email);

    if (success) {
        request.setAttribute("successMessage", "Movie deleted successfully!");
    } else {
        request.setAttribute("errorMessage", "Failed to delete movie.");
    }

    // Re-set auth data
    request.setAttribute("token", token);
    request.setAttribute("email", email);

    // Reload movies
    AddedMoviesXML moviesXML = Business.getMovies(token, email);
    request.setAttribute("movies", moviesXML);
    
    RequestDispatcher dispatcher = request.getRequestDispatcher("movies.jsp");
    dispatcher.forward(request, response);
}
}
