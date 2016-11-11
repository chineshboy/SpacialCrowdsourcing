package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.AssignmentHistory;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralWorker;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Created by jianxun on 16/5/3.
 */
public class WorkerSelectHeuriApproxi {
    private static Map<Integer, GeneralTask> taskMap;

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, SpatialIndex idx,
                                       AssignmentHistory history, long cTime) {
        taskMap = new HashMap<Integer, GeneralTask>();
        for (GeneralTask t : tasks) {
            taskMap.put(t.id, t);
        }
        AssignmentHistory tempHistory = new AssignmentHistory();
        List<WTMatch> result = new ArrayList<WTMatch>();
        // for each worker find the heuristic task sequence
        for (GeneralWorker w : workers) {
            List<Integer> taskIds = idx.intersects(w.region);
            Iterator<Integer> iter = taskIds.iterator();
            while (iter.hasNext()) {
                Integer tId = iter.next();
                if (history.has(w.id, tId)) {
                    iter.remove();
                } else if (taskMap.get(tId).getRequirement() <= tempHistory.getAssignedNumOfTask(tId)) {
                    iter.remove();
                }
            }
            // compare 3 heuristics and choose the maximum one
            List<Integer> tempResult = leastExpireTime(w.getCapacity(), w.location, taskIds, cTime);
            List<Integer> temp = nearestNeighbor(w.getCapacity(), w.location, taskIds, cTime);
            if (tempResult.size() < temp.size()) {
                tempResult = temp;
            }
            temp = mostPromising(w.getCapacity(), w.location, taskIds, cTime);
            if (tempResult.size() < temp.size()) {
                tempResult = temp;
            }
            for (Integer tid : tempResult) {
                result.add(new WTMatch(tid, w.id));
                tempHistory.add(w.id, tid);
            }
        }
        return result;
    }

    public static List<Integer> leastExpireTime(int capacity, Point p, List<Integer> taskIds, long time,
                                                Map<Integer, GeneralTask> taskMap) {
        WorkerSelectHeuriApproxi.taskMap = taskMap;
        return leastExpireTime(capacity, p, taskIds, time);
    }

    private static List<Integer> leastExpireTime(int capacity, Point p, List<Integer> taskIds, long time) {
        List<GeneralTask> tasks = new ArrayList<GeneralTask>();
        for (Integer id : taskIds) {
            tasks.add(taskMap.get(id));
        }
        Collections.sort(tasks, new Comparator<GeneralTask>() {
            @Override
            public int compare(GeneralTask o1, GeneralTask o2) {
                if (o1.expiryTime < o2.expiryTime) {
                    return -1;
                } else if (o1.expiryTime > o2.expiryTime) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        List<Integer> result = new ArrayList<Integer>();
        for (GeneralTask t : tasks) {
            long expectTime = WorkerSelectDP.travelCost(p, t.location) + time;
            if (result.size() < capacity && expectTime <= t.expiryTime) {
                result.add(t.id);
                p = t.location;
                time = expectTime;
            }
        }
        return result;
    }

    private static List<Integer> nearestNeighbor(int capacity, Point p, List<Integer> taskIds, long time) {
        List<GeneralTask> tasks = new ArrayList<GeneralTask>();
        for (Integer id : taskIds) {
            tasks.add(taskMap.get(id));
        }
        final Point s = p;
        Collections.sort(tasks, new Comparator<GeneralTask>() {
            @Override
            public int compare(GeneralTask o1, GeneralTask o2) {
                if (o1.location.distance(s) < o2.location.distance(s)) {
                    return -1;
                } else if (o1.location.distance(s) > o2.location.distance(s)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        List<Integer> result = new ArrayList<Integer>();
        for (GeneralTask t : tasks) {
            long expectTime = WorkerSelectDP.travelCost(p, t.location) + time;
            if (result.size() < capacity && expectTime <= t.expiryTime) {
                result.add(t.id);
                p = t.location;
                time = expectTime;
            }
        }
        return result;
    }

    private static List<Integer> mostPromising(int capacity, Point p, List<Integer> taskIds, long time) {
        // logger.info("[choose most promising one]");
        List<Integer> result = new ArrayList<Integer>();
        WorkerSelectBB.Node next = null;

        while (result.size() < capacity && !taskIds.isEmpty()) {
            Iterator<Integer> iter = taskIds.iterator();
            // choose the most promising one (with largest upper bound)
            while (iter.hasNext()) {
                // try each task as the next finished task
                Integer t = iter.next();
                // calculate the next level
                WorkerSelectBB.Node temp = WorkerSelectBB.calcUpperBound(t, p, taskIds, time, taskMap);
                // choose the most promising one
                if (temp != null && (next == null || temp.candidates.size() > next.candidates.size())) {
                    next = temp;
                }
                // can not be finished any more, delete
                if (temp == null) {
                    iter.remove();
                }
            }
            if (next != null) {
                taskIds.remove(next.id);
                result.add(next.id);
                time = next.time;
                p = taskMap.get(next.id).location;
                next = null;
            }
        }
        // logger.info("[end choose most promising]");
        return result;
    }

    public static List<Integer> mostPromising(int capacity, Point p, List<Integer> taskIds, long time,
                                                Map<Integer, GeneralTask> taskMap) {
        WorkerSelectHeuriApproxi.taskMap = taskMap;
        return mostPromising(capacity, p, taskIds, time);
    }
}
