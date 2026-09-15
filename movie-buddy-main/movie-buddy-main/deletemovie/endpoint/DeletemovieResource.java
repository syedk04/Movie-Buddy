package ryerson.ca.deletemovie.endpoint;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import ryerson.ca.deletemovie.business.DeleteMovieService;
import ryerson.ca.deletemovie.helper.DeleteResponse;

/**
 * REST Web Service for movie deletion
 */
@Path("delete")
public class DeletemovieResource {
    @Context
    private UriInfo context;
    private static final Logger logger = Logger.getLogger(DeletemovieResource.class.getName());

    /**
     * Creates a new instance of DeletemovieResource
     */
    public DeletemovieResource() {
    }

    /**
     * Handles the deletion of a movie.
     *
     * @param movieId The ID of the movie to delete.
     * @param email The email of the user requesting the deletion.
     * @return An XML response indicating success or failure.
     */
    @POST
    @Path("{movieId}/{email}")
    @Produces(MediaType.APPLICATION_XML + ";charset=utf-8")
    public Response deleteMovie(
            @PathParam("movieId") int movieId,
            @PathParam("email") String email) throws JAXBException {
        
        logger.log(Level.INFO, "Received delete request: movieId={0}, email={1}", 
                new Object[]{movieId, email});
        
        try {
            // Basic validation
            if (movieId <= 0) {
                DeleteResponse errorResponse = new DeleteResponse("error", "Invalid movie ID");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(objectToXML(errorResponse))
                        .build();
            }
            
            if (email == null || email.trim().isEmpty()) {
                DeleteResponse errorResponse = new DeleteResponse("error", "Invalid email address");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(objectToXML(errorResponse))
                        .build();
            }
            
            // Call the business layer to delete the movie
            boolean isDeleted = DeleteMovieService.deleteMovie(movieId, email);
            
            if (isDeleted) {
                // Return success response
                DeleteResponse successResponse = new DeleteResponse("message", "Movie deleted successfully");
                logger.log(Level.INFO, "Movie deleted successfully: movieId={0}, email={1}", 
                        new Object[]{movieId, email});
                return Response.ok(objectToXML(successResponse)).build();
            } else {
                // Return failure response
                DeleteResponse errorResponse = new DeleteResponse("error", 
                        "Failed to delete movie. Movie may not exist or user is not authorized.");
                logger.log(Level.WARNING, "Failed to delete movie: movieId={0}, email={1}", 
                        new Object[]{movieId, email});
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(objectToXML(errorResponse))
                        .build();
            }
        } catch (Exception e) {
            // Log the exception and return an internal server error response
            logger.log(Level.SEVERE, "Server error while deleting movie: movieId=" + movieId + 
                    ", email=" + email, e);
            DeleteResponse errorResponse = new DeleteResponse("error", "Server error: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(objectToXML(errorResponse))
                    .build();
        }
    }
    
    /**
     * Helper method to convert objects to XML string
     */
    private String objectToXML(DeleteResponse response) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(DeleteResponse.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        StringWriter sw = new StringWriter();
        jaxbMarshaller.marshal(response, sw);
        return sw.toString();
    }
}