package ryerson.ca.helper;

import java.util.List;

public class AddedMovies {

    private int movieID;
    private String title;
    private String genre;
    private String director;
    private List<User> users;

    public AddedMovies() {
    }

    public AddedMovies(int movieID, String title, String genre, String director) {
        this.movieID = movieID;
        this.title = title;
        this.genre = genre;
        this.director = director;
    }

    // Getters and setters
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

    public void setUsers(List<User> users) {
        this.users = users;
    }
}