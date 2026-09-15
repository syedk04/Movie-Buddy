package ryerson.ca.addservice.Helper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "movies")
@XmlAccessorType(XmlAccessType.FIELD)
public class AddedMoviesXML {
    @XmlElement(name = "movie") // Individual movie elements
    private List<AddedMovies> movies;

    // Default constructor
    public AddedMoviesXML() {
        this.movies = new ArrayList<>();
    }

    public AddedMoviesXML(List<AddedMovies> movies) {
        this.movies = movies;
    }

    // Getters and setters
    public List<AddedMovies> getMovies() {
        return movies;
    }

    public void setMovies(List<AddedMovies> movies) {
        this.movies = movies;
    }
}
