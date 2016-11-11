package com.HKUST.gMission.SpacialCroudsourcing.rtree;

import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Node implements Serializable {
    private static final long serialVersionUID = 72893472938649L;
    protected boolean changed;
    private int nodeId = 0;
    private Rectangle mbr;
    private List<Rectangle> entries = null;
    private int[] ids = null;
    private int level;
    private int entryCount;

    public Node(int nodeId, int level, int maxNodeEntries, int dimension) {
        this.init(nodeId, level, maxNodeEntries, dimension);
    }

    public void init(int nodeId, int level, int maxNodeEntries, int dimension) {
        this.nodeId = nodeId;
        this.level = level;
        this.entries = new ArrayList<Rectangle>(maxNodeEntries);
        this.ids = new int[maxNodeEntries];
        this.mbr = new Rectangle(dimension);
        this.entryCount = 0;
        this.changed = true;
    }

    public void addEntry(Rectangle r, int id) {
        ids[entryCount] = id;
        entries.add(r);
        mbr.add(r);
        entryCount++;
        this.changed = true;
    }

    public int findEntry(Rectangle r, int id) {
        for (int i = 0; i < entryCount; i++) {
            if (id == ids[i]) {
                Rectangle entry = entries.get(i);
                for (int j = 1; j <= r.getDimension(); j++) {
                    if (entry.getMin(j) != r.getMin(j) || entry.getMax(j) != r.getMax(j)) {
                        return -1;
                    }
                }
                return id;
            }
        }
        return -1;
    }

    public void deleteEntry(int i) {
        int lastIndex = entryCount - 1;
        Rectangle deleteR = entries.get(i);

        if (i != lastIndex) {
            entries.set(i, entries.get(lastIndex));
            entries.remove(lastIndex);
            ids[i] = ids[lastIndex];
        }
        entryCount--;

        // adjust the MBR
        recalculateMBRIfInfluencedBy(deleteR);
        this.changed = true;
    }

    public void recalculateMBRIfInfluencedBy(Rectangle r) {
        for (int i = 1; i <= r.getDimension(); i++) {
            if (mbr.getMin(i) == r.getMin(i) || mbr.getMax(i) == r.getMax(i)) {
                recalculateMBR();
                return;
            }
        }
    }

    public void recalculateMBR() {
        int dimension = mbr.getDimension();
        mbr = new Rectangle(dimension);

        for (Rectangle r : entries) {
            mbr.add(r);
        }
    }

    public int getEntryCount() {
        return entryCount;
    }

    public int getId(int index) {
        if (index < entryCount) {
            return ids[index];
        }
        return -1;
    }

    boolean isLeaf() {
        return (level == 1);
    }

    public int getLevel() {
        return level;
    }

    public Rectangle getBounds() {
        return mbr.copy();
    }

    public int getNodeId() {
        return nodeId;
    }

    public Rectangle getEntry(int i) {
        return entries.get(i).copy();
    }

    public void setEntry(int i, Rectangle r) {
        entries.set(i, r.copy());
        recalculateMBR();
        this.changed = true;
    }
}
