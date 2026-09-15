package ryerson.ca.addservice.endpoint;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import ryerson.ca.addservice.Business.AddBusiness;
import ryerson.ca.addservice.Helper.AddedMoviesXML;

@Path("movies")
public class AddResource {

    private static final Logger logger = Logger.getLogger(AddResource.class.getName());

    @Context
    private UriInfo context;

    public AddResource() {}

    /**
     * Retrieves all movies for a specific user.
     */
    @GET
    @Path("user")
    @javax.ws.rs.Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    public String getXml(@QueryParam("email") String email) {
        if (email == null || email.trim().isEmpty()) {
            return "<message>Email parameter is required</message>";
        }
        logger.info("Fetching movies for user: " + email);
        AddBusiness addBusiness = new AddBusiness();
        AddedMoviesXML moviesXML = addBusiness.getMoviesByUser(email);

        if (moviesXML == null || moviesXML.getMovies() == null || moviesXML.getMovies().isEmpty()) {
            return "<movies/>";
        }
        return marshalToXml(moviesXML);
    }

    /**
     * Searches movies for a user with optional filters.
     * Query params: email (required), title, genre, director (all optional).
     */
    @GET
    @Path("search")
    @javax.ws.rs.Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    public String searchMovies(
            @QueryParam("email") String email,
            @QueryParam("title") String titleFilter,
            @QueryParam("genre") String genreFilter,
            @QueryParam("director") String directorFilter) {

        if (email == null || email.trim().isEmpty()) {
            return "<message>Email parameter is required</message>";
        }
        logger.info("Searching movies for user: " + email
                + " [title=" + titleFilter + ", genre=" + genreFilter + ", director=" + directorFilter + "]");

        AddBusiness addBusiness = new AddBusiness();
        AddedMoviesXML moviesXML = addBusiness.searchMoviesWithFilters(
                email, titleFilter, genreFilter, directorFilter);

        if (moviesXML == null || moviesXML.getMovies() == null || moviesXML.getMovies().isEmpty()) {
            return "<movies/>";
        }
        return marshalToXml(moviesXML);
    }

    /**
     * Adds a new movie for a user.
     */
    @POST
    @Path("add")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @javax.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public Response addMovie(
            @FormParam("title") String title,
            @FormParam("genre") String genre,
            @FormParam("director") String director,
            @FormParam("email") String email) {

        if (isBlank(title) || isBlank(genre) || isBlank(director) || isBlank(email)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("All fields (title, genre, director, email) are required").build();
        }

        logger.info("Adding movie '" + title + "' for user: " + email);
        AddBusiness addBusiness = new AddBusiness();
        boolean isAdded = addBusiness.addMovie(title, genre, director, email);

        if (isAdded) {
            return Response.ok("Movie added successfully").build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Failed to add movie").build();
    }

    /**
     * Updates an existing movie's details.
     * Only the user who added the movie may update it.
     */
    @POST
    @Path("update/{movieId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @javax.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public Response updateMovie(
            @PathParam("movieId") int movieId,
            @FormParam("title") String title,
            @FormParam("genre") String genre,
            @FormParam("director") String director,
            @FormParam("email") String email) {

        if (movieId <= 0 || isBlank(title) || isBlank(genre) || isBlank(director) || isBlank(email)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Valid movieId and all fields are required").build();
        }

        logger.info("Updating movie " + movieId + " for user: " + email);
        AddBusiness addBusiness = new AddBusiness();
        boolean updated = addBusiness.updateMovie(movieId, title, genre, director, email);

        if (updated) {
            return Response.ok("Movie updated successfully").build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Update failed. Movie not found or you do not own it.").build();
    }

    /**
     * Rates a movie from 1 to 5 stars.
     * Only the user who added the movie may rate it.
     */
    @POST
    @Path("rate/{movieId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @javax.ws.rs.Produces(MediaType.TEXT_PLAIN)
    public Response rateMovie(
            @PathParam("movieId") int movieId,
            @FormParam("rating") int rating,
            @FormParam("email") String email) {

        if (movieId <= 0 || rating < 1 || rating > 5 || isBlank(email)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Valid movieId, rating (1-5), and email are required").build();
        }

        logger.info("Rating movie " + movieId + " with " + rating + " star(s) for user: " + email);
        AddBusiness addBusiness = new AddBusiness();
        boolean rated = addBusiness.rateMovie(movieId, rating, email);

        if (rated) {
            return Response.ok("Movie rated successfully").build();
        }
        return Response.status(Response.Status.BAD_REQUEST)
                .entity("Rating failed. Movie not found or you do not own it.").build();
    }

    private String marshalToXml(AddedMoviesXML moviesXML) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AddedMoviesXML.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            StringWriter sw = new StringWriter();
            marshaller.marshal(moviesXML, sw);
            return sw.toString();
        } catch (JAXBException ex) {
            logger.log(Level.SEVERE, "Error marshalling movies to XML", ex);
            return "<message>Error generating XML response</message>";
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
