package com.HKUST.gMission.SpacialCroudsourcing.model;

public class Distance implements Comparable<Distance> {
    private int id;
    private double dis;

    public Distance(int id) {
        this.setId(id);
        this.setDis(Double.POSITIVE_INFINITY);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getDis() {
        return dis;
    }

    public void setDis(double dis) {
        this.dis = dis;
    }

    @Override
    public int compareTo(Distance o) {
        if (this.dis < o.dis) {
            return -1;
        } else if (this.dis > o.dis) {
            return 1;
        } else {
            return 0;
        }
    }

}
