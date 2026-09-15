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
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.io.IOUtils;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;

public class Business {

    private static final Authenticate auth = new Authenticate();

    public static String authenticate(String username, String password) {
        String query = "SELECT Password FROM Users WHERE Username = ?";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("Password");
                // Compare the provided password with the stored hashed password
                if (password.equals(storedPassword)) { // Replace with proper password hashing logic
                    // Generate a JWT for the authenticated user
                    return auth.createJWT("FrontEnd", username, 86400000); // 24 hours
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Authentication failed
    }

    public static boolean registerUser(String username, String password, String email) {
        String query = "INSERT INTO Users (Username, Password, Email) VALUES (?, ?, ?)";
        try (Connection conn = DBConfig.getCon();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            stmt.setString(2, password); // Store hashed password in production
            stmt.setString(3, email);
            int rowsInserted = stmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static AddedMoviesXML getMovies(String token, String email) throws IOException {
    // Validate the JWT token
    try {
        if (!auth.verify(token).getKey()) {
            System.out.println("Token verification failed");
            throw new SecurityException("Invalid or expired token");
        }
        System.out.println("Token verified successfully");
        
        // Call AddService to fetch the list of movies
        Client client = ClientBuilder.newClient();
        String addService = System.getenv("addService");
        WebTarget target = client.target("http://"+addService+"/AddService/webresources/movies/user")
                .queryParam("email", email); // Add the email as a query parameter
        
        System.out.println("Calling AddService with email: " + email);
        
        Response response = target.request(MediaType.APPLICATION_XML)
                .header("Authorization", "Bearer " + token) // Pass the JWT in the request header
                .get();
        
        System.out.println("AddService response status: " + response.getStatus());
        
        if (response.getStatus() == 200) {
            InputStream is = response.readEntity(InputStream.class);
            String xml = IOUtils.toString(is, "utf-8");
            
            System.out.println("Received XML: " + xml);
            
            if (xml == null || xml.trim().isEmpty()) {
                System.out.println("Empty XML response");
                return new AddedMoviesXML(); // Return empty object instead of null
            }
            
            return parseMoviesXML(xml);
        } else {
            System.out.println("Failed to fetch movies. HTTP Status: " + response.getStatus());
            throw new RuntimeException("Failed to fetch movies. HTTP Status: " + response.getStatus());
        }
    } catch (Exception e) {
        System.out.println("Error in getMovies: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Error fetching movies: " + e.getMessage(), e);
    }
}
    
    private static AddedMoviesXML parseMoviesXML(String xml) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AddedMoviesXML.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (AddedMoviesXML) unmarshaller.unmarshal(new StringReader(xml));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }
    
   public static boolean addMovie(String token, String title, String genre, String director, String email) {
    try {
        // Validate the JWT token
        if (!auth.verify(token).getKey()) {
            System.out.println("Token verification failed");
            throw new SecurityException("Invalid or expired token");
        }
        System.out.println("Token verified successfully for adding movie");
        
        // Call AddService to add the movie
        Client client = ClientBuilder.newClient();
        String addService = System.getenv("addService");
        WebTarget target = client.target("http://"+addService+"/AddService/webresources/movies/add");
        
        // Create form data with email instead of userId
        Form form = new Form();
        form.param("title", title);
        form.param("genre", genre);
        form.param("director", director);
        form.param("email", email); // Pass email directly
        
        System.out.println("Calling AddService to add movie for email: " + email);
        
        Response response = target.request()
                .header("Authorization", "Bearer " + token)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        
        System.out.println("AddService response status for adding movie: " + response.getStatus());
        
        // Return true if the movie was added successfully (status code 200 or 201)
        return response.getStatus() == 200 || response.getStatus() == 201;
    } catch (Exception e) {
        System.out.println("Error in addMovie: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
    
    public static int getUserIdByEmail(String email) {
    String query = "SELECT UserID FROM Users WHERE Email = ?";
    try (Connection conn = DBConfig.getCon();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, email);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return rs.getInt("UserID");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return -1; // User not found}
    }
    
    public static boolean deleteMovie(String token, String movieId, String email) {
    try {
        // Validate the JWT token
        if (!auth.verify(token).getKey()) {
            System.out.println("Token verification failed");
            throw new SecurityException("Invalid or expired token");
        }
        System.out.println("Token verified successfully for deleting movie");
        
        // Call DeleteMovie microservice
        Client client = ClientBuilder.newClient();
        String deleteService = System.getenv("deleteService");
        WebTarget target = client.target("http://"+deleteService+"/DeleteMovie/webresources/delete/" + movieId + "/" + email);
        
        System.out.println("Calling DELETE service at: " + target.getUri());
        
        Response response = target.request(MediaType.APPLICATION_XML)
                .header("Authorization", "Bearer " + token)
                .post(Entity.text(""));
        
        System.out.println("DeleteMovie response status: " + response.getStatus());
        System.out.println("Response: " + response.readEntity(String.class));
        
        // Return true if the movie was deleted successfully
        return response.getStatus() == 200;
    } catch (Exception e) {
        System.out.println("Error in deleteMovie: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

}