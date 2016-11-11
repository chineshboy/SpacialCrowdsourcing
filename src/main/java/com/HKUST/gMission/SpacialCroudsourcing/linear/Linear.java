package com.HKUST.gMission.SpacialCroudsourcing.linear;

import com.HKUST.gMission.SpacialCroudsourcing.IntStack;
import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.model.Distance;

import java.io.*;
import java.util.*;

public class Linear implements SpatialIndex, Serializable {

    private static final long serialVersionUID = 94573847979283479L;

    private final static int DEFAULT_MAX_NODE_ENTRIES = 10000;
    private final static int DEFAULT_MIN_NODE_ENTRIES = 20;
    private int maxNodeEntries;
    private int minNodeEntries;

    private Map<Integer, Map<Integer, Rectangle>> nodeMap;

    private int size = 0;

    private int highestUsedNodeId = 0;
    private int newestUsedNodeId = 0;
    private IntStack deletedNodeIds = new IntStack();

    private String path;

    public static Linear ReadIndexFromDisk(String path) {
        String fullPath = path;
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        fullPath += "linear_index/index.data";
        File file = new File(fullPath);
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Linear ins = (Linear) ois.readObject();
            ois.close();
            fis.close();
            return ins;
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
    }

    @Override
    public void init(Properties props) {
        if (props == null) {
            maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
            minNodeEntries = DEFAULT_MIN_NODE_ENTRIES;
        } else {
            maxNodeEntries = Integer.parseInt(props.getProperty("MaxNodeEntries", "10000"));
            minNodeEntries = Integer.parseInt(props.getProperty("MinNodeEntries", "4000"));
        }

        if (maxNodeEntries < 1000) {
            maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
        }

        if (minNodeEntries < 1 || minNodeEntries > maxNodeEntries / 2) {
            minNodeEntries = maxNodeEntries / 2;
        }

        path = "./";
        if (props != null) {
            path = props.getProperty("indexpath", "./");
            if (!path.endsWith("/")) {
                path += "/";
            }
        }

        path += "linear_index/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }

        nodeMap = new HashMap<Integer, Map<Integer, Rectangle>>();
        nodeMap.put(0, new HashMap<Integer, Rectangle>());
    }

    @Override
    public void add(Rectangle r, int id) {
        Map<Integer, Rectangle> node = getNode(newestUsedNodeId);
        if (node.size() < maxNodeEntries) {
            node.put(id, r);
        } else {
            int newId = getNextNodeId();
            Map<Integer, Rectangle> newNode = new HashMap<Integer, Rectangle>();
            newNode.put(id, r);
            putNode(newId, newNode);
        }
        size++;
    }

    @Override
    public boolean delete(Rectangle p, int id) {
        int foundNodeId = -1;
        for (int nodeId : nodeMap.keySet()) {
            Map<Integer, Rectangle> node = getNode(nodeId);
            if (node.containsKey(id)) {
                Rectangle point = node.get(id);
                if (point.equals(p)) {
                    foundNodeId = nodeId;
                    break;
                }
            }
        }

        if (foundNodeId != -1) {
            Map<Integer, Rectangle> node = getNode(foundNodeId);
            node.remove(id);
            if (node.size() < minNodeEntries && foundNodeId != newestUsedNodeId) {
                for (int nodeId : node.keySet()) {
                    add(node.get(nodeId), nodeId);
                }
                removeNode(foundNodeId);
            }
        }

        return foundNodeId != -1;
    }

    @Override
    public List<Integer> nearest(Point p, double furthestDistance) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Integer> nearestN(Point p, int n, double furthestDistance) {
        List<Distance> disList = new ArrayList<Distance>();
        for (int nodeId : nodeMap.keySet()) {
            Map<Integer, Rectangle> node = getNode(nodeId);
            for (Integer id : node.keySet()) {
                Rectangle tp = node.get(id);
                Distance dis = new Distance(id);
                dis.setDis(tp.distance(p));
                if (dis.getDis() <= furthestDistance) {
                    disList.add(dis);
                }
            }
        }

        Collections.sort(disList);

        List<Integer> result = new ArrayList<Integer>();
        int i = 0;
        for (i = 0; i < n && i < disList.size(); i++) {
            result.add(disList.get(i).getId());
        }

        return result;
    }

    @Override
    public List<Integer> contains(Rectangle r) {
        List<Integer> result = new ArrayList<Integer>();
        for (int nodeId : nodeMap.keySet()) {
            Map<Integer, Rectangle> node = getNode(nodeId);
            for (Integer id : node.keySet()) {
                Rectangle p = node.get(id);
                if (r.contains(p)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Rectangle getBounds() {
        // TODO Auto-generated method stub
        return null;
    }

    private int getNextNodeId() {
        int nextNodeId = 0;
        if (deletedNodeIds.size() > 0) {
            nextNodeId = deletedNodeIds.pop();
        } else {
            nextNodeId = 1 + highestUsedNodeId++;
        }
        newestUsedNodeId = nextNodeId;
        return nextNodeId;
    }

    private Map<Integer, Rectangle> getNode(int id) {
        //return nodeMap.get(id);
        return getNodeFromDisk(id);
    }

    private void putNode(int id, Map<Integer, Rectangle> n) {
        nodeMap.put(id, n);
    }

    private void removeNode(int id) {
        nodeMap.remove(id);
        deletedNodeIds.push(id);
    }

    private Map<Integer, Rectangle> getNodeFromDisk(int id) {
        String fullPath = path + "index_" + id + ".data";
        File file = new File(fullPath);
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<Integer, Rectangle> ins = (Map<Integer, Rectangle>) ois.readObject();
            ois.close();
            fis.close();
            return ins;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void writeAllNodesIntoDisk() {
        for (int id : nodeMap.keySet()) {
            String fullPath = this.path + "index_" + id + ".data";
            File file = new File(fullPath);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(nodeMap.get(id));
                oos.close();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void WriteIndexIntoDisk() {
        String fullPath = this.path + "index.data";
        File file = new File(fullPath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeAllNodesIntoDisk();
    }

    @Override
    public List<Integer> intersects(Rectangle r) {
        List<Integer> result = new ArrayList<Integer>();
        for (int nodeId : nodeMap.keySet()) {
            Map<Integer, Rectangle> node = getNode(nodeId);
            for (Integer id : node.keySet()) {
                Rectangle tp = node.get(id);
                if (r.intersects(tp)) {
                    result.add(id);
                }
            }
        }
        return result;
    }
}
