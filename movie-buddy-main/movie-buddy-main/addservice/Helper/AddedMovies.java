package ryerson.ca.addservice.Helper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.util.List;

@XmlRootElement(name = "movie")
@XmlAccessorType(XmlAccessType.FIELD)
public class AddedMovies {

    private int movieID;
    private String title;
    private String genre;
    private String director;
    private int rating;

    @XmlTransient
    private List<User> users;

    public AddedMovies() {}

    public AddedMovies(int movieID, String title, String genre, String director) {
        this.movieID = movieID;
        this.title = title;
        this.genre = genre;
        this.director = director;
        this.rating = 0;
    }

    public int getMovieID() { return movieID; }
    public void setMovieID(int movieID) { this.movieID = movieID; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public List<User> getUsers() { return users; }

    public void addUsers(List<User> users) { this.users = users; }
}
