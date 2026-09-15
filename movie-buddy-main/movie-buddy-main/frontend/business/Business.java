package ryerson.ca.business;

import ryerson.ca.frontend.Authenticate;
import ryerson.ca.helper.AddedMoviesXML;
import ryerson.ca.persistence.DBConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;

public class Business {

    private static final Logger logger = Logger.getLogger(Business.class.getName());
    private static final Authenticate auth = new Authenticate();

    /**
     * Authenticates a user. Supports both hashed (new) and plaintext (legacy) passwords
     * to allow a seamless migration path.
     */
    public static String authenticate(String username, String password) {
        if (isBlank(username) || isBlank(password)) return null;

        String query = "SELECT Password FROM Users WHERE Username = ?";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String stored = rs.getString("Password");
                boolean match = PasswordUtil.verify(password, stored)   // hashed
                             || password.equals(stored);                  // legacy plaintext
                if (match) {
                    return auth.createJWT("FrontEnd", username, 86400000L); // 24 hours
                }
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Authentication query failed for user: " + username, e);
        }
        return null;
    }

    /**
     * Registers a new user with a hashed password.
     */
    public static boolean registerUser(String username, String password, String email) {
        if (isBlank(username) || isBlank(password) || isBlank(email)) return false;

        String query = "INSERT INTO Users (Username, Password, Email) VALUES (?, ?, ?)";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, PasswordUtil.hash(password));
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Registration failed for user: " + username, e);
            return false;
        }
    }

    /**
     * Retrieves all movies for the given user email via the AddService.
     */
    public static AddedMoviesXML getMovies(String token, String email) throws IOException {
        verifyToken(token);
        String addService = System.getenv("addService");
        WebTarget target = buildClient()
                .target("http://" + addService + "/AddService/webresources/movies/user")
                .queryParam("email", email);
        return fetchMoviesXml(target, token);
    }

    /**
     * Searches movies for a user with optional filters.
     */
    public static AddedMoviesXML searchMovies(
            String token, String email,
            String titleFilter, String genreFilter, String directorFilter) throws IOException {
        verifyToken(token);
        String addService = System.getenv("addService");
        WebTarget target = buildClient()
                .target("http://" + addService + "/AddService/webresources/movies/search")
                .queryParam("email", email);
        if (!isBlank(titleFilter))    target = target.queryParam("title", titleFilter);
        if (!isBlank(genreFilter))    target = target.queryParam("genre", genreFilter);
        if (!isBlank(directorFilter)) target = target.queryParam("director", directorFilter);
        return fetchMoviesXml(target, token);
    }

    public static boolean addMovie(String token, String title, String genre, String director, String email) {
        try {
            verifyToken(token);
            String addService = System.getenv("addService");
            WebTarget target = buildClient()
                    .target("http://" + addService + "/AddService/webresources/movies/add");

            Form form = new Form();
            form.param("title", title);
            form.param("genre", genre);
            form.param("director", director);
            form.param("email", email);

            Response response = target.request()
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            return response.getStatus() == 200 || response.getStatus() == 201;
        } catch (Exception e) {
            logger.log(Level.WARNING, "addMovie failed for email: " + email, e);
            return false;
        }
    }

    /**
     * Updates a movie's details via the AddService.
     * Only succeeds if the authenticated user owns the movie.
     */
    public static boolean updateMovie(String token, int movieId, String title, String genre,
                                      String director, String email) {
        try {
            verifyToken(token);
            String addService = System.getenv("addService");
            WebTarget target = buildClient()
                    .target("http://" + addService + "/AddService/webresources/movies/update/" + movieId);

            Form form = new Form();
            form.param("title", title);
            form.param("genre", genre);
            form.param("director", director);
            form.param("email", email);

            Response response = target.request()
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            return response.getStatus() == 200;
        } catch (Exception e) {
            logger.log(Level.WARNING, "updateMovie failed for movieId: " + movieId, e);
            return false;
        }
    }

    /**
     * Rates a movie from 1 to 5 stars via the AddService.
     * Only succeeds if the authenticated user owns the movie.
     */
    public static boolean rateMovie(String token, int movieId, int rating, String email) {
        try {
            verifyToken(token);
            if (rating < 1 || rating > 5) return false;

            String addService = System.getenv("addService");
            WebTarget target = buildClient()
                    .target("http://" + addService + "/AddService/webresources/movies/rate/" + movieId);

            Form form = new Form();
            form.param("rating", String.valueOf(rating));
            form.param("email", email);

            Response response = target.request()
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            return response.getStatus() == 200;
        } catch (Exception e) {
            logger.log(Level.WARNING, "rateMovie failed for movieId: " + movieId, e);
            return false;
        }
    }

    public static boolean deleteMovie(String token, String movieId, String email) {
        try {
            verifyToken(token);
            String deleteService = System.getenv("deleteService");
            WebTarget target = buildClient()
                    .target("http://" + deleteService + "/DeleteMovie/webresources/delete/"
                            + movieId + "/" + email);

            Response response = target.request(MediaType.APPLICATION_XML)
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.text(""));

            return response.getStatus() == 200;
        } catch (Exception e) {
            logger.log(Level.WARNING, "deleteMovie failed for movieId: " + movieId, e);
            return false;
        }
    }

    public static int getUserIdByEmail(String email) {
        String query = "SELECT UserID FROM Users WHERE Email = ?";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("UserID");
        } catch (SQLException e) {
            logger.log(Level.WARNING, "getUserIdByEmail failed for: " + email, e);
        }
        return -1;
    }

    // ---- helpers ----

    private static void verifyToken(String token) {
        try {
            if (!auth.verify(token).getKey()) {
                throw new SecurityException("Invalid or expired JWT token");
            }
        } catch (java.io.UnsupportedEncodingException e) {
            throw new SecurityException("Token verification error", e);
        }
    }

    private static Client buildClient() {
        return ClientBuilder.newClient();
    }

    private static AddedMoviesXML fetchMoviesXml(WebTarget target, String token) throws IOException {
        Response response = target.request(MediaType.APPLICATION_XML)
                .header("Authorization", "Bearer " + token)
                .get();

        if (response.getStatus() != 200) {
            throw new RuntimeException("AddService returned HTTP " + response.getStatus());
        }

        InputStream is = response.readEntity(InputStream.class);
        String xml = IOUtils.toString(is, "utf-8");

        if (xml == null || xml.trim().isEmpty() || xml.trim().equals("<movies/>")) {
            return new AddedMoviesXML();
        }
        return parseMoviesXML(xml);
    }

    private static AddedMoviesXML parseMoviesXML(String xml) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(AddedMoviesXML.class);
            Unmarshaller u = ctx.createUnmarshaller();
            return (AddedMoviesXML) u.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            logger.log(Level.SEVERE, "Failed to parse movies XML", e);
            return new AddedMoviesXML();
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
