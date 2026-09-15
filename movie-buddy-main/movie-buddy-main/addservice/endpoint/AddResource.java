package ryerson.ca.addservice.endpoint;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import ryerson.ca.addservice.Business.AddBusiness;
import ryerson.ca.addservice.Helper.AddedMoviesXML;

/**
 * REST Web Service
 *
 * @author student
 */
@Path("movies")
public class AddResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of AddResource
     */
    public AddResource() {
    }

    /**
     * Retrieves movies for a specific user.
     *
     * @param email The email of the user.
     * @return An XML representation of the user's movies.
     */
    @GET
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    @Path("user")
    public String getXml(@QueryParam("email") String email) {
        System.out.println("Fetching movies for user: " + email);
        AddBusiness addBusiness = new AddBusiness();
        AddedMoviesXML moviesXML = addBusiness.getMoviesByUser(email);

        if (moviesXML == null || moviesXML.getMovies().isEmpty()) {
            return "<message>No movies found for user</message>";
        }

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AddedMoviesXML.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            StringWriter sw = new StringWriter();
            jaxbMarshaller.marshal(moviesXML, sw);
            return sw.toString();
        } catch (JAXBException ex) {
            Logger.getLogger(AddResource.class.getName()).log(Level.SEVERE, null, ex);
            return "<message>Error generating XML response</message>";
        }
    }

    /**
     * Adds a new movie for a user.
     *
     * @param title    The title of the movie.
     * @param genre    The genre of the movie.
     * @param director The director of the movie.
     * @param userId   The ID of the user.
     * @return A message indicating success or failure.
     */
    @POST
    @Produces(MediaType.TEXT_HTML)
    @Path("add")
    public String addMovie(
            @FormParam("title") String title,
            @FormParam("genre") String genre,
            @FormParam("director") String director,
            @FormParam("email") String email) {

        System.out.println("Adding movie: " + title);
        AddBusiness addBusiness = new AddBusiness();
        boolean isAdded = addBusiness.addMovie(title, genre, director, email);

        if (isAdded) {
            return "Movie added successfully";
        } else {
            return "Failed to add movie";
        }
    }
}