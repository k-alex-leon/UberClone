package com.example.uberclone.models;

public class Client {

    private String idUser;
    private String username;
    private String email;
    private String imageProfile;
    private long timestamp;

    public Client(){}

    public Client(String idUser, String username, String email, String imageProfile, long timestamp) {
        this.idUser = idUser;
        this.username = username;
        this.email = email;
        this.imageProfile = imageProfile;
        this.timestamp = timestamp;
    }

    public String getIdUser() {
        return idUser;
    }

    public void setIdUser(String idUser) {
        this.idUser = idUser;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getImageProfile() {
        return imageProfile;
    }

    public void setImageProfile(String imageProfile) {
        this.imageProfile = imageProfile;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
