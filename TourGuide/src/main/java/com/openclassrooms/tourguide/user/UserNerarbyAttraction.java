package com.openclassrooms.tourguide.user;


//class allowing the creation of an object having the following attributes:
// Name of Tourist attraction, 
// Tourist attractions lat/long, 
// The user's location lat/long, 
// The distance in miles between the user's location and each of the attractions.
// The reward points for visiting each Attraction.

public class UserNerarbyAttraction {


    private String attractionName;
    private double attractLong;
    private double attractLat;
    private double userLong;
    private double userLat;
    private double distance;
    private int attractRewardPoints;
    public UserNerarbyAttraction(String attractionName, double attractLong, double attractLat, double userLong,
            double userLat, double distance, int attractRewardPoints) {
        this.attractionName = attractionName;
        this.attractLong = attractLong;
        this.attractLat = attractLat;
        this.userLong = userLong;
        this.userLat = userLat;
        this.distance = distance;
        this.attractRewardPoints = attractRewardPoints;
    }
    public UserNerarbyAttraction() {
        //TODO Auto-generated constructor stub
    }
    public void setAttractionName(String attractionName) {
        this.attractionName = attractionName;
    }
    public void setAttractLong(double attractLong) {
        this.attractLong = attractLong;
    }
    public void setAttractLat(double attractLat) {
        this.attractLat = attractLat;
    }
    public void setUserLong(double userLong) {
        this.userLong = userLong;
    }
    public void setUserLat(double userLat) {
        this.userLat = userLat;
    }
    public void setDistance(double distance) {
        this.distance = distance;
    }
    public void setAttractRewardPoints(int attractRewardPoints) {
        this.attractRewardPoints = attractRewardPoints;
    }
    public String getAttractionName() {
        return attractionName;
    }
    public double getAttractLong() {
        return attractLong;
    }
    public double getAttractLat() {
        return attractLat;
    }
    public double getUserLong() {
        return userLong;
    }
    public double getUserLat() {
        return userLat;
    }
    public double getDistance() {
        return distance;
    }
    public int getAttractRewardPoints() {
        return attractRewardPoints;
    }
}
