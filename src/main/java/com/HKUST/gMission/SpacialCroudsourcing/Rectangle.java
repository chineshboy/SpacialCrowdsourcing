package com.HKUST.gMission.SpacialCroudsourcing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Rectangle implements Serializable {

    private static final long serialVersionUID = 420485102390148L;
    private int dimension;
    private List<Double> minP, maxP;

    public Rectangle(List<Double> p1, List<Double> p2, int dimension) {
        this.dimension = dimension;
        this.minP = new ArrayList<Double>();
        this.maxP = new ArrayList<Double>();
        for (int i = 0; i < dimension; i++) {
            if (p1.get(i) > p2.get(i)) {
                this.minP.add(p2.get(i));
                this.maxP.add(p1.get(i));
            } else {
                this.minP.add(p1.get(i));
                this.maxP.add(p2.get(i));
            }
        }
    }

    public Rectangle(double minx, double miny, double maxx, double maxy) {
        this.dimension = 2;
        this.minP = new ArrayList<Double>();
        this.maxP = new ArrayList<Double>();
        this.minP.add(minx);
        this.minP.add(miny);
        this.maxP.add(maxx);
        this.maxP.add(maxy);
    }

    public Rectangle(int dimension) {
        this.dimension = dimension;
        this.minP = new ArrayList<Double>();
        this.maxP = new ArrayList<Double>();
        for (int i = 0; i < dimension; i++) {
            this.maxP.add(-Double.MAX_VALUE);
            this.minP.add(Double.MAX_VALUE);
        }
    }

    public static double enlargement(Rectangle r1, Rectangle r2) {
        double newArea = 1;
        for (int i = 0; i < r1.dimension; i++) {
            newArea *= Math.max(r1.maxP.get(i), r2.maxP.get(i)) -
                    Math.min(r1.minP.get(i), r2.minP.get(i));
        }
        return newArea - r1.area();
    }

    public void add(Point p) {
        for (int i = 1; i <= this.dimension; i++) {
            if (this.getMin(i) > p.getLocation(i)) {
                this.minP.set(i - 1, p.getLocation(i));
            }
            if (this.getMax(i) < p.getLocation(i)) {
                this.maxP.set(i - 1, p.getLocation(i));
            }
        }
    }

    public void add(Rectangle r) {
        for (int i = 1; i <= this.dimension; i++) {
            if (this.getMin(i) > r.getMin(i)) {
                this.minP.set(i - 1, r.getMin(i));
            }
            if (this.getMax(i) < r.getMax(i)) {
                this.maxP.set(i - 1, r.getMax(i));
            }
        }
    }

    public double getMin(int axis) {
        return minP.get(axis - 1);
    }

    public double getMax(int axis) {
        return maxP.get(axis - 1);
    }

    public int getDimension() {
        return this.dimension;
    }

    public boolean intersects(Rectangle r) {
        for (int i = 1; i <= this.dimension; i++) {
            if (this.getMax(i) < r.getMin(i) || this.getMin(i) > r.getMax(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Point p) {
        for (int i = 1; i <= this.dimension; i++) {
            if (this.getMin(i) > p.getLocation(i) || this.getMax(i) < p.getLocation(i)) {
                return false;
            }
        }
        return true;
    }

    public boolean contains(Rectangle r) {
        for (int i = 1; i <= this.dimension; i++) {
            if (this.getMin(i) > r.getMin(i) || this.getMax(i) < r.getMax(i)) {
                return false;
            }
        }
        return true;
    }

    public double distance(Point p) {
        double disSquared = 0;
        double temp;
        for (int i = 1; i <= this.dimension; i++) {
            temp = this.getMin(i) - p.getLocation(i);
            if (temp < 0) {
                temp = p.getLocation(i) - this.getMax(i);
            }
            if (temp > 0) {
                disSquared += temp * temp;
            }
        }
        return Math.sqrt(disSquared);
    }

    public double area() {
        double a = 1;
        for (int i = 1; i <= this.dimension; i++) {
            a *= this.getMax(i) - this.getMin(i);
        }
        return a;
    }

    public Rectangle copy() {
        return new Rectangle(minP, maxP, dimension);
    }

    public boolean equals(Rectangle r) {
        for (int i = 1; i <= dimension; i++) {
            if (this.getMax(i) != r.getMax(i) || this.getMin(i) != r.getMin(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "{minP: " + minP + ", maxP: " + maxP + "}";
    }
}
