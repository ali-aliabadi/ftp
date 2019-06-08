package server;

public class User {

    private String username;
    private String password;
    private boolean isOnline;


    User(String username, String password) {

        this.username = username;
        this.password = password;
        this.isOnline = true;

    }

    String getUsername() {
        return username;
    }

    String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return username;
    }

    boolean isOnline() {
        return isOnline;
    }

    void setOnline(boolean online) {
        isOnline = online;
    }
}
