package com.HKUST.gMission.SpacialCroudsourcing.rtree;

import com.HKUST.gMission.SpacialCroudsourcing.*;
import com.HKUST.gMission.SpacialCroudsourcing.PriorityQueue;

import java.io.*;
import java.util.*;

public class RTree implements SpatialIndex, Serializable {

    private static final long serialVersionUID = 8751934851987L;

    private final static int DEFAULT_MAX_NODE_ENTRIES = 50;
    private final static int DEFAULT_MIN_NODE_ENTRIES = 20;
    // internal consistency checking - set to true if debugging tree corruption
    private final static boolean INTERNAL_CONSISTENCY_CHECKING = false;
    // initialisation
    private final static int leafLevel = 1;// leaves are always level 1
    private int maxNodeEntries;
    private int minNodeEntries;
    // map of nodeId -> node object
    // TODO eliminate this map - it should not be needed. Nodes
    // can be found by traversing the tree.
    private HashMap<Integer, Node> nodeMap = new HashMap<Integer, Node>();
    // stores nodes that newly put into map or read from disk
    private Map<Integer, Node> writeBuffer = new HashMap<Integer, Node>();
    // index file path
    private String path;
    // stacks used to store nodeId and entry index of each node
    // from the root down to the leaf. Enables fast lookup
    // of nodes when a split is propagated up the tree.
    private IntStack parents = new IntStack();
    private IntStack parentsEntry = new IntStack();
    private int treeHeight = 1;
    private int rootNodeId = 0;
    private int size = 0;

    // Enables creation of new nodes
    private int highestUsedNodeId = rootNodeId;

    // Deleted node objects are retained in the nodeMap,
    // so that they can be reused. Store the IDs of nodes
    // which can be reused.
    private IntStack deletedNodeIds = new IntStack();

    public static RTree ReadIndexFromDisk(String path) {
        String fullPath = path;
        if (!fullPath.endsWith("/")) {
            fullPath += "/";
        }
        fullPath += "rtree_index/index.data";
        File file = new File(fullPath);
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            RTree ins = (RTree) ois.readObject();
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
            maxNodeEntries = Integer.parseInt(props.getProperty("MaxNodeEntries", "0"));
            minNodeEntries = Integer.parseInt(props.getProperty("MinNodeEntries", "0"));
        }
        // force maxNodeEntries to be at least 2 times larger than
        // minNodeEntires.
        if (maxNodeEntries < 2) {
            maxNodeEntries = DEFAULT_MAX_NODE_ENTRIES;
        }

        if (minNodeEntries < 1 || minNodeEntries > maxNodeEntries / 2) {
            minNodeEntries = maxNodeEntries / 2;
        }

        // entryStatus = new byte[maxNodeEntries];
        // initialEntryStatus = new byte[maxNodeEntries];

        // for (int i = 0; i < maxNodeEntries; i++) {
        // initialEntryStatus[i] = ENTRY_STATUS_UNASSIGNED;
        // }

        int dimension = 2;
        if (props != null) {
            dimension = Integer.parseInt(props.getProperty("dimension", "2"));
        }

        path = "./";
        if (props != null) {
            path = props.getProperty("indexpath", "./");
            if (!path.endsWith("/")) {
                path += "/";
            }
        }

        path += "rtree_index/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdir();
        }

        Node root = new Node(rootNodeId, 1, maxNodeEntries, dimension);
        putNode(rootNodeId, root);
    }

    @Override
    public void add(Rectangle r, int id) {
        add(r.copy(), id, leafLevel);
        size++;
        //writeChangedNodesIntoDisk();
    }

    @Override
    public boolean delete(Rectangle r, int id) {
        parents.clear();
        parents.push(rootNodeId);

        parentsEntry.clear();
        parentsEntry.push(-1);
        Node n = null;
        int foundIndex = -1; // index of entry to be deleted in leaf

        while (foundIndex == -1 && parents.size() > 0) {
            n = getNode(parents.peek());
            int startIndex = parentsEntry.peek() + 1;

            if (!n.isLeaf()) {

                boolean contains = false;
                for (int i = startIndex; i < n.getEntryCount(); i++) {
                    if (n.getEntry(i).contains(r)) {
                        parents.push(n.getId(i));
                        parentsEntry.pop();
                        parentsEntry.push(i); // this becomes the start index
                        // when the child has been
                        // searched
                        parentsEntry.push(-1);
                        contains = true;
                        break; // ie go to next iteration of while()
                    }
                }
                if (contains) {
                    continue;
                }
            } else {
                foundIndex = n.findEntry(r, id);
            }

            parents.pop();
            parentsEntry.pop();
        } // while not found

        if (foundIndex != -1 && n != null) {
            n.deleteEntry(foundIndex);
            condenseTree(n);
            size--;
        }

        // shrink the tree if possible (i.e. if root node has exactly one
        // entry,and that
        // entry is not a leaf node, delete the root (it's entry becomes the new
        // root)
        Node root = getNode(rootNodeId);
        while (root.getEntryCount() == 1 && treeHeight > 1) {
            deletedNodeIds.push(rootNodeId);
            rootNodeId = root.getId(0);
            treeHeight--;
            root = getNode(rootNodeId);
        }

        // if the tree is now empty, then set the MBR of the root node back to
        // it's original state
        // (this is only needed when the tree is empty, as this is the only
        // state where an empty node
        // is not eliminated)
        if (size == 0) {
            root.recalculateMBR();
        }

        if (INTERNAL_CONSISTENCY_CHECKING) {
            checkConsistency();
        }

        //writeChangedNodesIntoDisk();
        return (foundIndex != -1);
    }

    private void add(Rectangle r, int id, int level) {
        Node n = chooseNode(r, level);
        Node newLeaf = null;

        // I2 [Add record to leaf node] If L has room for another entry,
        // install E. Otherwise invoke SplitNode to obtain L and LL containing
        // E and all the old entries of L
        if (n.getEntryCount() < maxNodeEntries) {
            n.addEntry(r, id);
        } else {
            newLeaf = splitNode(n, r, id);
        }

        // I3 [Propagate changes upwards] Invoke AdjustTree on L, also passing
        // LL
        // if a split was performed
        Node newNode = adjustTree(n, newLeaf);

        // I4 [Grow tree taller] If node split propagation caused the root to
        // split, create a new root whose children are the two resulting nodes.
        if (newNode != null) {
            int oldRootNodeId = rootNodeId;
            Node oldRoot = getNode(oldRootNodeId);

            rootNodeId = getNextNodeId();
            treeHeight++;
            Node root = new Node(rootNodeId, treeHeight, maxNodeEntries, r.getDimension());
            root.addEntry(newNode.getBounds(), newNode.getNodeId());
            root.addEntry(oldRoot.getBounds(), oldRoot.getNodeId());
            putNode(rootNodeId, root);
        }
    }

    private int getNextNodeId() {
        int nextNodeId = 0;
        if (deletedNodeIds.size() > 0) {
            nextNodeId = deletedNodeIds.pop();
        } else {
            nextNodeId = 1 + highestUsedNodeId++;
        }
        return nextNodeId;
    }

    private Node adjustTree(Node n, Node nn) {
        while (n.getLevel() != treeHeight) {

            // AT3 [Adjust covering rectangle in parent entry] Let P be the
            // parent
            // node of N, and let En be N's entry in P. Adjust EnI so that it
            // tightly
            // encloses all entry rectangles in N.
            Node parent = getNode(parents.pop());
            int entry = parentsEntry.pop();

            if (!parent.getEntry(entry).equals(n.getBounds())) {
                parent.setEntry(entry, n.getBounds());
            }

            // AT4 [Propagate node split upward] If N has a partner NN resulting
            // from
            // an earlier split, create a new entry Enn with Ennp pointing to NN
            // and
            // Enni enclosing all rectangles in NN. Add Enn to P if there is
            // room.
            // Otherwise, invoke splitNode to produce P and PP containing Enn
            // and
            // all P's old entries.
            Node newNode = null;
            if (nn != null) {
                if (parent.getEntryCount() < maxNodeEntries) {
                    parent.addEntry(nn.getBounds(), nn.getNodeId());
                } else {
                    newNode = splitNode(parent, nn.getBounds(), nn.getNodeId());
                }
            }

            // AT5 [Move up to next level] Set N = P and set NN = PP if a split
            // occurred. Repeat from AT2
            n = parent;
            nn = newNode;

            parent = null;
            newNode = null;
        }

        return nn;
    }

    // split node n into two nodes: n and newNode, the new node would be
    // returned
    private Node splitNode(Node n, Rectangle newRect, int newId) {
        Node newNode = new Node(getNextNodeId(), n.getLevel(), maxNodeEntries, newRect.getDimension());
        putNode(newNode.getNodeId(), newNode);

        // get all the entries and ids
        List<Rectangle> entries = new ArrayList<Rectangle>();
        int count = n.getEntryCount();
        List<Integer> ids = new ArrayList<Integer>();

        for (int i = 0; i < count; i++) {
            entries.add(n.getEntry(i));
            ids.add(n.getId(i));
        }

        entries.add(newRect);
        ids.add(newId);
        count++;

        // set n into initial status
        n.init(n.getNodeId(), n.getLevel(), maxNodeEntries, newRect.getDimension());
        // insert initial entry into each node
        pickSeeds(n, newNode, entries, ids);

        // assign all the entries into two nodes, satisfying min and max
        // restrictions
        while (!entries.isEmpty()) {
            if (entries.size() + n.getEntryCount() == minNodeEntries) {
                // assign all remaining entries to original node
                for (int i = 0; i < entries.size(); i++) {
                    n.addEntry(entries.get(i), ids.get(i));
                }
                break;
            }
            if (entries.size() + newNode.getEntryCount() == minNodeEntries) {
                // assign all remaining entries to new node
                for (int i = 0; i < entries.size(); i++) {
                    newNode.addEntry(entries.get(i), ids.get(i));
                }
                break;
            }

            // pick one entry to add into one node
            pickNext(n, newNode, entries, ids);
        }

        return newNode;
    }

    // pick the next entry to add into one node, the entry and id are then
    // removed
    private void pickNext(Node n, Node nn, List<Rectangle> entries, List<Integer> ids) {
        double minAreaInc = Double.MAX_VALUE;
        int minIncIndex = -1;
        Node toBeAdd = null;

        // pick the entry with min increase area
        for (int i = 0; i < entries.size(); i++) {
            double areaInc = Rectangle.enlargement(n.getBounds(), entries.get(i));
            if (areaInc < minAreaInc) {
                areaInc = minAreaInc;
                minIncIndex = i;
                toBeAdd = n;
            }

            areaInc = Rectangle.enlargement(nn.getBounds(), entries.get(i));
            if (areaInc < minAreaInc) {
                areaInc = minAreaInc;
                minIncIndex = i;
                toBeAdd = nn;
            }
        }

        toBeAdd.addEntry(entries.get(minIncIndex), ids.get(minIncIndex));
        entries.remove(minIncIndex);
        ids.remove(minIncIndex);
    }

    // pick two entries into two nodes, chosen nodes and ids are removed
    private void pickSeeds(Node n, Node nn, List<Rectangle> entries, List<Integer> ids) {
        // find the bottom-left entry
        int lowestSumIndex = -1;
        double lowest = Double.MAX_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            Rectangle entry = entries.get(i);
            double tempLow = 0;
            for (int j = 1; j <= entry.getDimension(); j++) {
                tempLow += entry.getMin(j);
            }
            if (tempLow < lowest) {
                lowest = tempLow;
                lowestSumIndex = i;
            }
        }

        // lowestSumIndex is the seed for the node n.
        n.addEntry(entries.get(lowestSumIndex), ids.get(lowestSumIndex));
        ids.remove(lowestSumIndex);
        entries.remove(lowestSumIndex);

        // find the top-right entry
        int highestSumIndex = -1;
        double highest = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (i != lowestSumIndex) {
                Rectangle entry = entries.get(i);
                double tempHigh = 0;
                for (int j = 1; j <= entry.getDimension(); j++) {
                    tempHigh += entry.getMax(j);
                }
                if (tempHigh > highest) {
                    highest = tempHigh;
                    highestSumIndex = i;
                }
            }
        }

        // highestSumIndex is the seed for the node nn.
        nn.addEntry(entries.get(highestSumIndex), ids.get(highestSumIndex));
        ids.remove(highestSumIndex);
        entries.remove(highestSumIndex);
    }

    private Node chooseNode(Rectangle p, int level) {
        // CL1 [Initialize] Set N to be the root node
        Node n = getNode(rootNodeId);
        parents.clear();
        parentsEntry.clear();

        // CL2 [Leaf check] If N is a leaf, return N
        while (true) {
            if (n.getLevel() == level) {
                return n;
            }

            // choose rectangle with least enlargement to add. Resolve
            // ties by choosing the entry with the rectangle of smaller area.
            double leastEnlargement = Rectangle.enlargement(n.getEntry(0), p);
            int index = 0; // index of rectangle in subtree
            for (int i = 1; i < n.getEntryCount(); i++) {
                double tempEnlargement = Rectangle.enlargement(n.getEntry(i), p);
                if (tempEnlargement < leastEnlargement
                        || (tempEnlargement == leastEnlargement && n.getEntry(i).area() < n.getEntry(index).area())) {
                    index = i;
                    leastEnlargement = tempEnlargement;
                }
            }

            parents.push(n.getNodeId());
            parentsEntry.push(index);

            // CL4 [Descend until a leaf is reached] Set N to be the child node
            // pointed to by Fp and repeat from CL2
            n = getNode(n.getId(index));
        }
    }

    private void checkConsistency() {
        // TODO Auto-generated method stub

    }

    private void condenseTree(Node n) {
        // CT1 [Initialize] Set n=l. Set the list of eliminated
        // nodes to be empty.
        Node parent = null;
        int parentEntry = 0;

        IntStack eliminatedNodeIds = new IntStack();

        // CT2 [Find parent entry] If N is the root, go to CT6. Otherwise
        // let P be the parent of N, and let En be N's entry in P
        while (n.getLevel() != treeHeight) {
            parent = getNode(parents.pop());
            parentEntry = parentsEntry.pop();

            // CT3 [Eliminiate under-full node] If N has too few entries,
            // delete En from P and add N to the list of eliminated nodes
            if (n.getEntryCount() < minNodeEntries) {
                parent.deleteEntry(parentEntry);
                eliminatedNodeIds.push(n.getNodeId());
            } else {
                // if n's mbr has changed, recalculate its entry in parent
                if (!n.getBounds().equals(parent.getEntry(parentEntry))) {
                    parent.setEntry(parentEntry, n.getBounds());
                }
            }
            // CT5 [Move up one level in tree] Set N=P and repeat from CT2
            n = parent;
        }

        // Reinsert all entries of eliminated nodes with the same level
        while (eliminatedNodeIds.size() > 0) {
            Node e = getNode(eliminatedNodeIds.pop());
            for (int j = 0; j < e.getEntryCount(); j++) {
                add(e.getEntry(j), e.getId(j), e.getLevel());
            }
            e.init(e.getNodeId(), e.getLevel(), maxNodeEntries, e.getBounds().getDimension());
            deletedNodeIds.push(e.getNodeId());
        }
    }

    @Override
    public List<Integer> nearest(Point p, double furthestDistance) {
        Node rootNode = getNode(rootNodeId);
        List<Integer> result = new ArrayList<Integer>();
        nearest(p, rootNode, furthestDistance, result);
        return result;
    }

    private double nearest(Point p, Node n, double furthestDistance, List<Integer> result) {
        for (int i = 0; i < n.getEntryCount(); i++) {
            double tempDistance = n.getEntry(i).distance(p);
            if (n.isLeaf()) { // for leaves, the distance is an actual nearest
                // distance
                if (tempDistance < furthestDistance) {
                    furthestDistance = tempDistance;
                    result.clear();
                }
                if (tempDistance <= furthestDistance) {
                    result.add(n.getId(i));
                }
            } else { // for index nodes, only go into them if they potentially
                // could have
                // a rectangle nearer than actualNearest
                if (tempDistance <= furthestDistance) {
                    // search the child node
                    furthestDistance = nearest(p, getNode(n.getId(i)), furthestDistance, result);
                }
            }
        }
        return furthestDistance;
    }

    @Override
    public List<Integer> nearestN(Point p, int n, double furthestDistance) {
        List<Integer> result = new ArrayList<Integer>();
        PriorityQueue distanceQueue = new PriorityQueue(PriorityQueue.SORT_ORDER_DESCENDING);
        createNearestNDistanceQueue(p, n, distanceQueue, furthestDistance);
        distanceQueue.setSortOrder(PriorityQueue.SORT_ORDER_ASCENDING);

        while (distanceQueue.size() > 0) {
            result.add(distanceQueue.getValue());
            distanceQueue.pop();
        }
        return result;
    }

    private void createNearestNDistanceQueue(Point p, int n, PriorityQueue distanceQueue, double furthestDistance) {
        // return immediately if given an invalid "count" parameter
        if (n <= 0) {
            return;
        }

        IntStack parents = new IntStack();
        parents.push(rootNodeId);

        IntStack parentsEntry = new IntStack();
        parentsEntry.push(-1);

        List<Integer> savedValues = new ArrayList<Integer>();
        double savedPriority = 0;

        // TODO: possible shortcut here - could test for intersection with the
        // MBR of the root node. If no intersection, return immediately.

        while (parents.size() > 0) {
            Node node = getNode(parents.peek());
            int startIndex = parentsEntry.peek() + 1;

            if (!node.isLeaf()) {
                // go through every entry in the index node to check
                // if it could contain an entry closer than the farthest entry
                // currently stored.
                boolean near = false;
                for (int i = startIndex; i < node.getEntryCount(); i++) {
                    if (node.getEntry(i).distance(p) <= furthestDistance) {
                        parents.push(node.getId(i));
                        parentsEntry.pop();
                        parentsEntry.push(i); // this becomes the start index
                        // when the child has been
                        // searched
                        parentsEntry.push(-1);
                        near = true;
                        break; // ie go to next iteration of while()
                    }
                }
                if (near) {
                    continue;
                }
            } else {
                // go through every entry in the leaf to check if
                // it is currently one of the nearest N entries.
                for (int i = 0; i < node.getEntryCount(); i++) {
                    double entryDistance = node.getEntry(i).distance(p);
                    int entryId = node.getId(i);

                    if (entryDistance <= furthestDistance) {
                        distanceQueue.insert(entryId, entryDistance);

                        while (distanceQueue.size() > n) {
                            // normal case - we can simply remove the lowest
                            // priority (highest distance) entry
                            int value = distanceQueue.getValue();
                            double distance = distanceQueue.getPriority();
                            distanceQueue.pop();

                            // rare case - multiple items of the same priority
                            // (distance)
                            if (distance == distanceQueue.getPriority()) {
                                savedValues.add(value);
                                savedPriority = distance;
                            } else {
                                savedValues.clear();
                            }
                        }

                        // if the saved values have the same distance as the
                        // next one in the tree, add them back in.
                        if (savedValues.size() > 0 && savedPriority == distanceQueue.getPriority()) {
                            for (int svi = 0; svi < savedValues.size(); svi++) {
                                distanceQueue.insert(savedValues.get(svi), savedPriority);
                            }
                            savedValues.clear();
                        }

                        // narrow the search, if we have already found N items
                        if (distanceQueue.getPriority() < furthestDistance && distanceQueue.size() >= n) {
                            furthestDistance = distanceQueue.getPriority();
                        }
                    }
                }
            }
            parents.pop();
            parentsEntry.pop();
        }
    }

    @Override
    public List<Integer> contains(Rectangle r) {
        IntStack parents = new IntStack();
        parents.push(rootNodeId);

        IntStack parentsEntry = new IntStack();
        parentsEntry.push(-1);

        List<Integer> result = new ArrayList<Integer>();

        // TODO: possible shortcut here - could test for intersection with the
        // MBR of the root node. If no intersection, return immediately.

        while (parents.size() > 0) {
            Node n = getNode(parents.peek());
            int startIndex = parentsEntry.peek() + 1;

            if (!n.isLeaf()) {
                // go through every entry in the index node to check
                // if it intersects the passed rectangle. If so, it
                // could contain entries that are contained.
                boolean intersects = false;
                for (int i = startIndex; i < n.getEntryCount(); i++) {
                    if (r.intersects(n.getEntry(i))) {
                        parents.push(n.getId(i));
                        parentsEntry.pop();
                        parentsEntry.push(i); // this becomes the start index
                        // when the child has been
                        // searched
                        parentsEntry.push(-1);
                        intersects = true;
                        break; // ie go to next iteration of while()
                    }
                }
                if (intersects) {
                    continue;
                }
            } else {
                // go through every entry in the leaf to check if
                // it is contained by the passed rectangle
                for (int i = 0; i < n.getEntryCount(); i++) {
                    if (r.contains(n.getEntry(i))) {
                        result.add(n.getId(i));
                    }
                }
            }
            parents.pop();
            parentsEntry.pop();
        }

        return result;
    }

    @Override
    public List<Integer> intersects(Rectangle r) {
        Node rootNode = getNode(rootNodeId);
        List<Integer> result = new ArrayList<Integer>();
        intersects(r, result, rootNode);
        return result;
    }

    private boolean intersects(Rectangle r, List<Integer> v, Node n) {
        for (int i = 0; i < n.getEntryCount(); i++) {
            if (r.intersects(n.getEntry(i))) {
                if (n.isLeaf()) {
                    v.add(n.getId(i));
                } else {
                    Node childNode = getNode(n.getId(i));
                    if (!intersects(r, v, childNode)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Rectangle getBounds() {
        Node n = getNode(rootNodeId);
        if (n != null && n.getEntryCount() > 0) {
            return n.getBounds();
        }
        return null;
    }

    private Node getNode(int id) {
        if (nodeMap.containsKey(id)) {
            return nodeMap.get(id);
        } else if (writeBuffer.containsKey(id)) {
            return writeBuffer.get(id);
        } else {
            Node n = getNodeFromDisk(id);
            //writeBuffer.put(id, n);
            return n;
        }
    }

    private void putNode(int id, Node n) {
        if (n.isLeaf()) {
            writeBuffer.put(id, n);
        } else {
            nodeMap.put(id, n);
        }
    }

    private void writeChangedNodesIntoDisk() {
        for (int id : writeBuffer.keySet()) {
            Node n = writeBuffer.get(id);
            if (n.changed) {
                n.changed = false;
                String fullPath = path + "index_" + id + ".data";
                File file = new File(fullPath);
                try {
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    FileOutputStream fos = new FileOutputStream(file);
                    ObjectOutputStream oos = new ObjectOutputStream(fos);
                    oos.writeObject(n);
                    oos.close();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        writeBuffer.clear();
    }

    private Node getNodeFromDisk(int id) {
        String fullPath = path + "index_" + id + ".data";
        File file = new File(fullPath);
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Node ins = (Node) ois.readObject();
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
        writeChangedNodesIntoDisk();
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
    }
}
