package ryerson.ca.frontend;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger logger = Logger.getLogger(FrontEnd.class.getName());
    private static final String AUTH_COOKIE = "login_token";
    private static final int COOKIE_MAX_AGE = 86400; // 24 hours

    private final Authenticate auth = new Authenticate();

    private Entry<String, String> isAuthenticated(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String token = "";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (AUTH_COOKIE.equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (!token.isEmpty()) {
            try {
                Entry<Boolean, String> result = auth.verify(token);
                if (result.getKey()) {
                    return new AbstractMap.SimpleEntry<>(token, result.getValue());
                }
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, "Token verification error", e);
            }
        }

        return new AbstractMap.SimpleEntry<>("", "");
    }

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Entry<String, String> authResult = isAuthenticated(request);
        String token    = authResult.getKey();
        String username = authResult.getValue();
        String action   = request.getParameter("action");

        if (action == null) {
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
            case "search":
                handleSearch(request, response, token, username);
                break;
            default:
                response.sendRedirect("login.jsp");
                break;
        }
    }

    private void handleLogin(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String token = Business.authenticate(username, password);

        if (token != null) {
            Cookie cookie = new Cookie(AUTH_COOKIE, token);
            cookie.setHttpOnly(true);
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setPath("/");
            response.addCookie(cookie);
            response.sendRedirect("FrontEnd?action=movies&email=" + username);
        } else {
            request.setAttribute("error", "Invalid username or password");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private void handleMovies(HttpServletRequest request, HttpServletResponse response,
                               String token, String username)
            throws ServletException, IOException {

        if (token.isEmpty()) {
            response.sendRedirect("login.jsp");
            return;
        }

        String email = getUserEmail(username);
        AddedMoviesXML moviesXML = Business.getMovies(token, email);

        request.setAttribute("username", username);
        request.setAttribute("movies", moviesXML);
        request.setAttribute("token", token);
        request.setAttribute("email", email);
        request.getRequestDispatcher("movies.jsp").forward(request, response);
    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response,
                               String token, String username)
            throws ServletException, IOException {

        if (token.isEmpty()) {
            response.sendRedirect("login.jsp");
            return;
        }

        String email          = getUserEmail(username);
        String titleFilter    = request.getParameter("title");
        String genreFilter    = request.getParameter("genre");
        String directorFilter = request.getParameter("director");

        AddedMoviesXML moviesXML = Business.searchMovies(token, email, titleFilter, genreFilter, directorFilter);

        request.setAttribute("username", username);
        request.setAttribute("movies", moviesXML);
        request.setAttribute("token", token);
        request.setAttribute("email", email);
        request.setAttribute("searchTitle", titleFilter);
        request.setAttribute("searchGenre", genreFilter);
        request.setAttribute("searchDirector", directorFilter);
        request.getRequestDispatcher("movies.jsp").forward(request, response);
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
            handleDeleteMovie(request, response);
        } else if (uri.contains("/updatemovie")) {
            handleUpdateMovie(request, response);
        } else if (uri.contains("/ratemovie")) {
            handleRateMovie(request, response);
        } else {
            processRequest(request, response);
        }
    }

    private void handleAddMovie(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String title    = request.getParameter("title");
        String genre    = request.getParameter("genre");
        String director = request.getParameter("director");
        String email    = request.getParameter("email");
        String token    = resolveToken(request);

        if (isBlank(title) || isBlank(genre) || isBlank(director) || isBlank(email) || isBlank(token)) {
            request.setAttribute("errorMessage", "All fields are required");
            forwardToMovies(request, response, token, email);
            return;
        }

        boolean success = Business.addMovie(token, title, genre, director, email);
        request.setAttribute(success ? "successMessage" : "errorMessage",
                             success ? "Movie added successfully!" : "Failed to add movie.");
        forwardToMovies(request, response, token, email);
    }

    private void handleDeleteMovie(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String movieId = request.getParameter("movieId");
        String email   = request.getParameter("email");
        String token   = resolveToken(request);

        if (isBlank(movieId) || isBlank(email) || isBlank(token)) {
            request.setAttribute("errorMessage", "Invalid delete request");
            forwardToMovies(request, response, token, email);
            return;
        }

        boolean success = Business.deleteMovie(token, movieId, email);
        request.setAttribute(success ? "successMessage" : "errorMessage",
                             success ? "Movie deleted successfully!" : "Failed to delete movie.");
        forwardToMovies(request, response, token, email);
    }

    private void handleUpdateMovie(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String movieIdParam = request.getParameter("movieId");
        String title        = request.getParameter("title");
        String genre        = request.getParameter("genre");
        String director     = request.getParameter("director");
        String email        = request.getParameter("email");
        String token        = resolveToken(request);

        if (isBlank(movieIdParam) || isBlank(title) || isBlank(genre)
                || isBlank(director) || isBlank(email) || isBlank(token)) {
            request.setAttribute("errorMessage", "All fields are required to update a movie");
            forwardToMovies(request, response, token, email);
            return;
        }

        int movieId;
        try {
            movieId = Integer.parseInt(movieIdParam);
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Invalid movie ID");
            forwardToMovies(request, response, token, email);
            return;
        }

        boolean success = Business.updateMovie(token, movieId, title, genre, director, email);
        request.setAttribute(success ? "successMessage" : "errorMessage",
                             success ? "Movie updated successfully!" : "Failed to update movie.");
        forwardToMovies(request, response, token, email);
    }

    private void handleRateMovie(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String movieIdParam = request.getParameter("movieId");
        String ratingParam  = request.getParameter("rating");
        String email        = request.getParameter("email");
        String token        = resolveToken(request);

        if (isBlank(movieIdParam) || isBlank(ratingParam) || isBlank(email) || isBlank(token)) {
            request.setAttribute("errorMessage", "Movie ID, rating, and email are required");
            forwardToMovies(request, response, token, email);
            return;
        }

        int movieId, rating;
        try {
            movieId = Integer.parseInt(movieIdParam);
            rating  = Integer.parseInt(ratingParam);
        } catch (NumberFormatException e) {
            request.setAttribute("errorMessage", "Invalid movie ID or rating");
            forwardToMovies(request, response, token, email);
            return;
        }

        boolean success = Business.rateMovie(token, movieId, rating, email);
        request.setAttribute(success ? "successMessage" : "errorMessage",
                             success ? "Movie rated successfully!" : "Failed to rate movie. Rating must be 1-5.");
        forwardToMovies(request, response, token, email);
    }

    private void forwardToMovies(HttpServletRequest request, HttpServletResponse response,
                                  String token, String email)
            throws ServletException, IOException {

        if (!isBlank(token) && !isBlank(email)) {
            request.setAttribute("token", token);
            request.setAttribute("email", email);
            try {
                AddedMoviesXML moviesXML = Business.getMovies(token, email);
                request.setAttribute("movies", moviesXML);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not reload movies after action", e);
            }
        }
        request.getRequestDispatcher("movies.jsp").forward(request, response);
    }

    private String getUserEmail(String username) {
        String query = "SELECT Email FROM Users WHERE Username = ?";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getString("Email");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Could not look up email for user: " + username, e);
        }
        return username;
    }

    private String resolveToken(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (!isBlank(token)) return token;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (AUTH_COOKIE.equals(c.getName())) return c.getValue();
            }
        }
        return "";
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
