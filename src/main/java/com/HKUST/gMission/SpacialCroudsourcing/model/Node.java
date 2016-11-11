package com.HKUST.gMission.SpacialCroudsourcing.model;

public class Node {
    private int ID;
    private double longitude;
    private double latitude;

    public int getID() {
        return ID;
    }

    public void setID(Integer iD) {
        ID = iD;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    @Override
    public String toString() {
        return "ID: " + this.ID + ",longitude: " + this.longitude + ",latitude: " + this.latitude;
    }
}
