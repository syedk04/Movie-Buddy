package ryerson.ca.helper;

import java.util.ArrayList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "movies")
public class AddedMoviesXML {

    private ArrayList<AddedMovies> movies;

    public AddedMoviesXML() {
    }

    public AddedMoviesXML(ArrayList<AddedMovies> movies) {
        this.movies = movies;
    }

    @XmlElement(name = "movie")
    public ArrayList<AddedMovies> getMovies() {
        return movies;
    }

    public void setMovies(ArrayList<AddedMovies> movies) {
        this.movies = movies;
    }
}