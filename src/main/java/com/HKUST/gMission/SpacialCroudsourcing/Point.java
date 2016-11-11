package com.HKUST.gMission.SpacialCroudsourcing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Point implements Serializable {

    private static final long serialVersionUID = 47204198193823L;
    private List<Double> location;

    public Point(List<Double> location) {
        this.location = new ArrayList<Double>(location);
    }

    public Point(double longitude, double latitude) {
        this.location = new ArrayList<Double>();
        this.location.add(longitude);
        this.location.add(latitude);
    }

    public int getDimension() {
        return this.location.size();
    }

    public boolean setLocation(List<Double> location) {
        if (this.location.size() != location.size()) {
            return false;
        }
        this.location = new ArrayList<Double>(location);
        return true;
    }

    public void setLocation(int axis, double v) {
        this.location.set(axis - 1, v);
    }

    public double getLocation(int axis) {
        return this.location.get(axis - 1);
    }

    public double distance(Point p) {
        double disSquared = 0;
        double temp;
        for (int i = 1; i <= this.location.size(); i++) {
            temp = this.getLocation(i) - p.getLocation(i);
            disSquared += temp * temp;
        }
        return Math.sqrt(disSquared);
    }

    public boolean equals(Point p) {
        for (int i = 1; i <= location.size(); i++) {
            if (location.get(i) != p.getLocation(i)) {
                return false;
            }
        }
        return true;
    }
}
