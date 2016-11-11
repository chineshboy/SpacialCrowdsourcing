package com.HKUST.gMission.SpacialCroudsourcing.action;


import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.*;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import com.HKUST.gMission.SpacialCroudsourcing.linear.Linear;
import com.HKUST.gMission.SpacialCroudsourcing.rtree.RTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RequestHandler {
    public static Logger logger = LogManager.getLogger();

    private SpatialIndex rtreeIndex;
    private SpatialIndex linearIndex;
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    public RequestHandler(String path) {
        logger.info("[INIT RTREE]");
        rtreeIndex = RTree.ReadIndexFromDisk(path);
        if (rtreeIndex == null) {
            logger.info("[no rtree index on disk, initialize a new one]");
            rtreeIndex = new RTree();
            Properties p = new Properties();
            p.put("MaxNodeEntries", "100");
            p.put("MinNodeEntries", "40");
            p.put("indexpath", path);
            p.put("dimension", "2");
            rtreeIndex.init(p);
        }

        logger.info("[FINISH INIT RTREE]");
        logger.info("[INIT LINEAR]");
        linearIndex = Linear.ReadIndexFromDisk(path);
        if (linearIndex == null) {
            logger.info("[no linear index on disk, initialize a new one]");
            linearIndex = new Linear();
            Properties p = new Properties();
            p.put("MaxNodeEntries", "100");
            p.put("MinNodeEntries", "40");
            p.put("indexpath", path);
            linearIndex.init(p);
        }
        logger.info("[FINISH INIT LINEAR]");
    }

    public void PutNode(float longitude, float latitude, int id) {
        // if the node already exists, delete it first, then add the new
        // position.
    /*Point oldPoint = rectsById.get(new Integer(id));
	if (oldPoint != null) {
	    rtreeIndex.delete(oldPoint, id);
	}*/
        List<Double> loc = new ArrayList<Double>();
        loc.add((double) longitude);
        loc.add((double) latitude);
        Rectangle newRec = new Rectangle(loc, loc, 2);
        linearIndex.add(newRec, id);
        rtreeIndex.add(newRec, id);
    }

    public Point getNode(int id) {
        //return rectsById.get(new Integer(id));
        return null;
    }

    public void DeleteNode(float longitude, float latitude, int id) {
        List<Double> loc = new ArrayList<Double>();
        loc.add((double) longitude);
        loc.add((double) latitude);
        Rectangle rect = new Rectangle(loc, loc, 2);
        rtreeIndex.delete(rect, id);
        linearIndex.delete(rect, id);
    }

    public List<Integer> ContainsByRect(float longitude1, float latitude1, float longitude2, float latitude2) {
        List<Double> p1 = new ArrayList<Double>();
        p1.add((double) longitude1);
        p1.add((double) latitude1);
        List<Double> p2 = new ArrayList<Double>();
        p2.add((double) longitude2);
        p2.add((double) latitude2);
        Rectangle rect = new Rectangle(p1, p2, 2);
        return rtreeIndex.contains(rect);
    }

    public List<Integer> LinearContainsByRect(float longitude1, float latitude1, float longitude2, float latitude2) {
        List<Double> p1 = new ArrayList<Double>();
        p1.add((double) longitude1);
        p1.add((double) latitude1);
        List<Double> p2 = new ArrayList<Double>();
        p2.add((double) longitude2);
        p2.add((double) latitude2);
        Rectangle rect = new Rectangle(p1, p2, 2);
        List<Integer> result = linearIndex.contains(rect);
        return result;
    }

    public List<Integer> NearestK(float longitude, float latitude, int k, float furthest) {
        List<Double> loc = new ArrayList<Double>();
        loc.add((double) longitude);
        loc.add((double) latitude);
        Point p = new Point(loc);
        return rtreeIndex.nearestN(p, k, furthest);
    }

    public List<Integer> LinearNearestK(float longitude, float latitude, int k, float furthest) {
        List<Double> loc = new ArrayList<Double>();
        loc.add((double) longitude);
        loc.add((double) latitude);
        Point p = new Point(loc);
        List<Integer> result = linearIndex.nearestN(p, k, furthest);
        return result;
    }

    public void writeIntoDisk() {
        ((RTree) rtreeIndex).WriteIndexIntoDisk();
        ((Linear) linearIndex).WriteIndexIntoDisk();
    }

    private List<WTMatch> assignTasks(String method, long currentTime, List<GeneralTask> tasks,
                                      List<GeneralWorker> workers, AssignmentHistory history) {



        logger.info("[task num: " + tasks.size() + "][worker num: " + workers.size() + "]");
        RTree index = new RTree();
        initialInnerIndex(index);
        List<WTMatch> result = new ArrayList<WTMatch>();
        long startTime = System.currentTimeMillis();
        if (method.equals("geocrowdgreedy")) {
            for (GeneralWorker w : workers) {
                index.add(w.region, w.id);
            }
            logger.info("[ASSIGN GEOCROWD_GREEDY]");
            startTime = System.currentTimeMillis();
            result = GeoCrowdGreedy.assign(tasks, workers, index, history);
        } else if (method.equals("geocrowdllep")) {
            for (GeneralWorker w : workers) {
                index.add(w.region, w.id);
            }
            logger.info("[ASSIGN GEOCROWD_LLEP]");
            startTime = System.currentTimeMillis();
            result = GeoCrowdLLEP.assign(tasks, workers, index, history);
        } else if (method.equals("geocrowdnnp")) {
            for (GeneralWorker w : workers) {
                index.add(w.region, w.id);
            }
            logger.info("[ASSIGN GEOCROWD_NNP]");
            startTime = System.currentTimeMillis();
            result = GeoCrowdNNP.assign(tasks, workers, index, history);
        } else if (method.equals("geotrucrowdgreedy")) {
            logger.info("[ASSIGN GEOTRUCROWD_GREEDY]");
            result = GeoTruCrowdGreedy.assign(tasks, workers, history);
        } else if (method.equals("geotrucrowdlo")) {
            logger.info("[ASSIGN GEOTRUCROWD_LO]");
            result = GeoTruCrowdLO.assign(tasks, workers, history);
        } else if (method.equals("rdbscdivideandconquer")) {
            logger.info("[ASSIGN RDBSC_D&C]");
            result = new RDB_SC_DivideAndConquer().assign(tasks, workers, 5, currentTime, "0.9", "0.00001", history);
        } else if (method.equals("rdbscsampling")) {
            logger.info("[ASSIGN RDBSC_SAMPLING]");
            result = RDB_SC_Sampling.assign(tasks, workers, currentTime, "0.9", "0.00001", history);
        } else if (method.equals("geotrucrowdhgr")) {
            logger.info("[ASSIGN GEOTRUCROWD_HGR]");
            result = GeoTruCrowdHGR.assign(tasks, workers, history);
        } else if (method.equals("workerselectdp")) {
            logger.info("[ASSIGN WORKER SELECT DP]");
            for (GeneralTask t : tasks) {
                index.add(new Rectangle(t.longitude, t.latitude, t.longitude, t.latitude), t.id);
            }
            startTime = System.currentTimeMillis();
            result = WorkerSelectDP.assign(tasks, workers, index, history, currentTime);
        } else if (method.equals("workerselectbb")) {
            for (GeneralTask t : tasks) {
                index.add(new Rectangle(t.longitude, t.latitude, t.longitude, t.latitude), t.id);
            }
            logger.info("[ASSIGN WORKER SELECT BB]");
            startTime = System.currentTimeMillis();
            result = WorkerSelectBB.assign(tasks, workers, index, history, currentTime);
        } else if (method.equals("workerselectha")) {
            for (GeneralTask t : tasks) {
                index.add(new Rectangle(t.longitude, t.latitude, t.longitude, t.latitude), t.id);
            }
            logger.info("[ASSIGN WORKER SELECT HEURISTIC APPROXIMATE]");
            startTime = System.currentTimeMillis();
            result = WorkerSelectHeuriApproxi.assign(tasks, workers, index, history, currentTime);
        } else if (method.equals("workerselectprogressive")) {
            for (GeneralTask t : tasks) {
                index.add(new Rectangle(t.longitude, t.latitude, t.longitude, t.latitude), t.id);
            }
            logger.info("[ASSIGN WORKER SELECT PROGRESSIVE]");
            startTime = System.currentTimeMillis();
            result = WorkerSelectProgressive.assign(tasks, workers, index, history, currentTime);
        }

        long endTime = System.currentTimeMillis();
        result.add(new WTMatch(-1, endTime - startTime));
        logger.info("[FINISHED ASSIGN][" + (endTime - startTime) / 1000.0 + "s]");
        System.gc();
        logger.info("[manually gc done]");
        return result;
    }

    public List<WTMatch> assignTasks(String method, long currentTime) {
        List<GeneralTask> tasks = GeneralTask.queryOpenTasksFromDB(jdbcTemplate, currentTime);
        List<GeneralWorker> workers = GeneralWorker.queryActiveWorkersFromDB(jdbcTemplate);
        AssignmentHistory history = new AssignmentHistory(jdbcTemplate);
        return assignTasks(method, currentTime, tasks, workers, history);
    }

    public List<WTMatch> assignTasksBatch(String method) {
        List<List<GeneralTask>> tasks = GeneralTask.queryAllTasksFromDB(jdbcTemplate);
        List<List<GeneralWorker>> workers = GeneralWorker.queryAllWorkersFromDB(jdbcTemplate);
        AssignmentHistory history = new AssignmentHistory();
        List<WTMatch> result = new ArrayList<WTMatch>();
        for (int i = 0; i < tasks.size(); i++) {
            for (GeneralTask t : tasks.get(i)) {
                t.assignedCount = history.getAssignedNumOfTask(t.id);
            }
            for (GeneralWorker w : workers.get(i)) {
                w.assigned = history.getAssignedNumOfWorker(w.id);
            }
            List<WTMatch> temp = assignTasks(method, i, tasks.get(i), workers.get(i), history);
            result.addAll(temp);
            for (WTMatch m: temp) {
                if (m.taskId != -1) {
                    history.add(m.workerId, m.taskId);
                }
            }
        }
        return result;
    }

    public void initialInnerIndex(SpatialIndex index) {
        Properties p = new Properties();
        p.put("MaxNodeEntries", "100");
        p.put("MinNodeEntries", "40");
        p.put("indexpath", ".");
        p.put("dimension", "2");
        index.init(p);
    }

}
