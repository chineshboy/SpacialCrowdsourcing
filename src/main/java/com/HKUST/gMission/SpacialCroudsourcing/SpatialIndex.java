package com.HKUST.gMission.SpacialCroudsourcing;

import java.util.List;
import java.util.Properties;

public interface SpatialIndex {

    public void init(Properties props);

    public void add(Rectangle r, int id);

    public boolean delete(Rectangle r, int id);

    public List<Integer> nearest(Point p, double furthestDistance);

    public List<Integer> nearestN(Point p, int n, double furthestDistance);

    public List<Integer> contains(Rectangle r);

    public List<Integer> intersects(Rectangle r);

    public int size();

    public Rectangle getBounds();
}
