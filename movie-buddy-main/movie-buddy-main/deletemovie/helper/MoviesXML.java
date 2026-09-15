package ryerson.ca.deletemovie.helper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "movies")
@XmlAccessorType(XmlAccessType.FIELD)
public class MoviesXML {

    @XmlElement(name = "movie")
    private List<Movie> movies;

    public MoviesXML() {
        this.movies = new ArrayList<>();
    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }
}
