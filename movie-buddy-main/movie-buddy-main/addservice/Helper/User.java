package ryerson.ca.addservice.Helper;

public class User {
    private int userID;
    private String email;

    public User(int userID, String email) {
        this.userID = userID;
        this.email = email;
    }

    // Getters and setters
    public int getUserID() {
        return userID;
    }

    public String getEmail() {
        return email;
    }
}