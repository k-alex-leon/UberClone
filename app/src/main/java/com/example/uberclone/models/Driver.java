package com.example.uberclone.models;

public class Driver {

    private String idUser;
    private String username;
    private String email;
    private String imageProfile;
    private String vehicleBrand;
    private String vehiclePlate;
    private long timestamp;

    public Driver(){}

    public Driver(String idUser, String username, String email, String imageProfile,
                  String vehicleBrand, String vehiclePlate, long timestamp) {
        this.idUser = idUser;
        this.username = username;
        this.email = email;
        this.imageProfile = imageProfile;
        this.vehicleBrand = vehicleBrand;
        this.vehiclePlate = vehiclePlate;
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

    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public void setVehiclePlate(String vehiclePlate) {
        this.vehiclePlate = vehiclePlate;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
