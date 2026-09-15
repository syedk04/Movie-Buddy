package ryerson.ca.deletemovie.helper;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "movie")
@XmlAccessorType(XmlAccessType.FIELD)
public class Movie {

    private int movieID;
    private String title;
    private String genre;
    private String director;

    @XmlElementWrapper(name = "users")
    @XmlElement(name = "user")
    private List<User> users;  // Users who added the movie

    public Movie() {
        this.users = new ArrayList<>();
    }

    public Movie(int movieID, String title, String genre, String director) {
        this.movieID = movieID;
        this.title = title;
        this.genre = genre;
        this.director = director;
        this.users = new ArrayList<>();
    }

    public int getMovieID() {
        return movieID;
    }

    public void setMovieID(int movieID) {
        this.movieID = movieID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public List<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        this.users.add(user);
    }

    @Override
    public String toString() {
        return "Movie{" +
                "movieID=" + movieID +
                ", title='" + title + '\'' +
                ", genre='" + genre + '\'' +
                ", director='" + director + '\'' +
                ", users=" + users +
                '}';
    }
}
