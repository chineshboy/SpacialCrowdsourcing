package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.assign.module.AssignmentHistory;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralWorker;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class RDB_SC_DivideAndConquer {

    private static final double KMeansEndThreshold = 0.01;

    private long currentTime;

    private int minSize;

    private String portion;

    private String confidence;

    private AssignmentHistory history;

    public static Logger logger = LogManager.getLogger();

    private Map<Integer, GeneralTask> taskMap;

    private Map<Integer, GeneralWorker> workerMap;

    public List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, int minSize,
                                long currentTime, String portion, String confidence, AssignmentHistory history) {
        this.currentTime = currentTime;
        this.minSize = minSize;
        this.portion = portion;
        this.confidence = confidence;
        this.history = history;
        this.taskMap = new HashMap<Integer, GeneralTask>();
        this.workerMap = new HashMap<Integer, GeneralWorker>();
        for (GeneralTask t : tasks) {
            taskMap.put(t.id, t);
        }
        for (GeneralWorker w : workers) {
            workerMap.put(w.id, w);
        }
        return assign(tasks, workers);
    }

    private List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers) {
        if (tasks.size() <= minSize) {
            List<WTMatch> t =  RDB_SC_Sampling.assign(tasks, workers, currentTime, portion, confidence, history);
//            logger.info("[RDB SC Divide and Conquer][Sampling Done]");
            return t;
        }
        DivideResult divided = divide(tasks, workers);
//        logger.info("[RDB SC Divide and Conquer][Divided][" + divided.tasks1.size() + " " + divided.workers1.size()
//         + "][" + divided.tasks2.size() + " " + divided.workers2.size() + "]");
        List<WTMatch> matches1 = assign(divided.tasks1, divided.workers1);
        List<WTMatch> matches2 = assign(divided.tasks2, divided.workers2);
        return merge(matches1, matches2, workers, history);
    }

    private DivideResult divide(List<GeneralTask> tasks, List<GeneralWorker> workers) {
        DivideResult result = partitionTasksWithKMeans(tasks);
        for (GeneralWorker w : workers) {
            if (canDoSomeTasks(w, result.tasks1)) {
                result.workers1.add(w);
            }
            if (canDoSomeTasks(w, result.tasks2)) {
                result.workers2.add(w);
            }
        }
        return result;
    }

    private boolean canDoSomeTasks(GeneralWorker w, List<GeneralTask> tasks) {
        for (GeneralTask t : tasks) {
            if (RDB_SC_Sampling.canReach(w, t, currentTime, history)) {
                return true;
            }
        }
        return false;
    }

    private DivideResult partitionTasksWithKMeans(List<GeneralTask> tasks) {
        // logger.info("[kmeans in]");
        int halfsize = tasks.size() / 2;
        double center1x, center2x, center1y, center2y;
        center1x = tasks.get(0).longitude;
        center2x = tasks.get(1).longitude;
        center1y = tasks.get(0).latitude;
        center2y = tasks.get(1).latitude;
        List<GeneralTask> group1 = new ArrayList<GeneralTask>();
        List<GeneralTask> group2 = new ArrayList<GeneralTask>();
        int calcTime = 0;
        while (calcTime < 20) {
            calcTime += 1;
            group1.clear();
            group2.clear();
            for (GeneralTask t : tasks) {
                double distance1 = (t.longitude - center1x) * (t.longitude - center1x)
                        + (t.latitude - center1y) * (t.latitude - center1y);
                double distance2 = (t.longitude - center2x) * (t.longitude - center2x)
                        + (t.latitude - center2y) * (t.latitude - center2y);
                if (group2.size() > halfsize
                        || (group1.size() < halfsize && distance1 < distance2)) {
                    group1.add(t);
                } else {
                    group2.add(t);
                }
            }
            double sum1x = 0, sum2x = 0, sum1y = 0, sum2y = 0;
            for (GeneralTask t : group1) {
                sum1x += t.longitude;
                sum1y += t.latitude;
            }
            for (GeneralTask t : group2) {
                sum2x += t.longitude;
                sum2y += t.latitude;
            }
            double new1x = sum1x / group1.size();
            double new1y = sum1y / group1.size();
            double new2x = sum2x / group2.size();
            double new2y = sum2y / group2.size();
            if (change(center1x, center1y, new1x, new1y) < KMeansEndThreshold
                    && change(center2x, center2y, new2x, new2y) < KMeansEndThreshold) {
                break;
            }
            center1x = new1x;
            center1y = new1y;
            center2x = new2x;
            center2y = new2y;
        }
        DivideResult result = new DivideResult();
        result.tasks1 = group1;
        result.tasks2 = group2;
        // logger.info("[kmeans out]");
        return result;
    }

    private double change(double x1, double y1, double x2, double y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    private List<WTMatch> merge(List<WTMatch> matches1, List<WTMatch> matches2, List<GeneralWorker> workers,
                                AssignmentHistory history) {
        Map<Integer, Integer> workerLoad = new HashMap<Integer, Integer>();
        List<Integer> workerIds = new ArrayList<Integer>();
        AssignmentHistory current = new AssignmentHistory();
        // calculate workload and add to current record
        for (WTMatch match : matches1) {
            current.add(match.workerId, match.taskId);
            if (workerLoad.containsKey(match.workerId)) {
                workerLoad.put(match.workerId, workerLoad.get(match.workerId) + 1);
            } else {
                workerLoad.put(match.workerId, 1);
                workerIds.add(match.workerId);
            }
        }
        for (WTMatch match : matches2) {
            current.add(match.workerId, match.taskId);
            if (workerLoad.containsKey(match.workerId)) {
                workerLoad.put(match.workerId, workerLoad.get(match.workerId) + 1);
            } else {
                workerLoad.put(match.workerId, 1);
                workerIds.add(match.workerId);
            }
        }
        // check each worker
        while (!workerIds.isEmpty()) {
            // check each worker
            int workerId = workerIds.remove(0);
            GeneralWorker worker = workerMap.get(workerId);
            while (workerLoad.get(workerId) > worker.getCapacity()) {
                int taskId = 0;
                // find an arbitrary task to remove
                Iterator<WTMatch> iter = matches1.iterator();
                while (iter.hasNext()) {
                    WTMatch match = iter.next();
                    if (match.workerId == workerId) {
                        taskId = match.taskId;
                        iter.remove();
                        break;
                    }
                }
                // if not found in matches1, find in matches2
                iter = matches2.iterator();
                if (taskId == 0) {
                    while (iter.hasNext()) {
                        WTMatch match = iter.next();
                        if (match.workerId == workerId) {
                            taskId = match.taskId;
                            iter.remove();
                            break;
                        }
                    }
                }
                current.remove(workerId, taskId);
                workerLoad.put(workerId, workerLoad.get(workerId) - 1);
                if (taskId == 0) {
                    logger.info("!!!!!something wrong!!!!!");
                }
                // try to find another worker to finish this task
                WTMatch newMatch = tryMakeUp(taskMap.get(taskId), workerLoad, workers, current, history);
                if (newMatch != null) {
                    int wId = newMatch.workerId;
                    if (workerLoad.containsKey(wId)) {
                        workerLoad.put(wId, workerLoad.get(wId) + 1);
                    } else {
                        workerLoad.put(wId, 1);
                    }
                    current.add(wId, taskId);
                    matches1.add(newMatch);
                }
            }
        }
        List<WTMatch> result = new ArrayList<WTMatch>();
        result.addAll(matches1);
        result.addAll(matches2);
        return result;
    }

    private WTMatch tryMakeUp(GeneralTask t, Map<Integer, Integer> workLoads, List<GeneralWorker> workers,
                              AssignmentHistory current, AssignmentHistory history) {
        WTMatch match = null;
        for (GeneralWorker w : workers) {
//            if (w == null || workLoads.get(w.id) == null) {
//                logger.info("found null worker");
//            }
            if (w.getCapacity() == 0 || (workLoads.containsKey(w.id) && w.getCapacity() <= workLoads.get(w.id))) {
                continue;
            }
            if ((!current.has(w.id, t.id)) && current.getAssignedNumOfTask(t.id) < t.getRequirement()
                    && RDB_SC_Sampling.canReach(w, t, currentTime, history)) {
                match = new WTMatch(t.id, w.id);
                break;
            }
        }
        return match;
    }

    private class DivideResult {

        public List<GeneralTask> tasks1;
        public List<GeneralWorker> workers1;
        public List<GeneralTask> tasks2;
        public List<GeneralWorker> workers2;
        public DivideResult() {
            tasks1 = new ArrayList<GeneralTask>();
            tasks2 = new ArrayList<GeneralTask>();
            workers1 = new ArrayList<GeneralWorker>();
            workers2 = new ArrayList<GeneralWorker>();
        }
    }
}
