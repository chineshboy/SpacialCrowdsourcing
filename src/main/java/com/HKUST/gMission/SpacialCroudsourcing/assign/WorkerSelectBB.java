package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * branch and bound
 * Created by jianxun on 16/5/3.
 */
public class WorkerSelectBB {
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
        // for each worker fine the maximum task sequence
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

            tempResult.clear();
            leastBound = 0;
            search(w, w.location, taskIds, new ArrayList<Integer>(), cTime);
            for (Integer tid : tempResult) {
                result.add(new WTMatch(tid, w.id));
                tempHistory.add(w.id, tid);
            }
        }
        return result;
    }

    public static List<Integer> search(GeneralWorker w, Point location, List<Integer> taskIds, List<Integer> finished, long time, Map<Integer, GeneralTask> taskMap) {
        WorkerSelectBB.taskMap = taskMap;
        tempResult.clear();
        leastBound = 0;
        search(w, location, taskIds, finished, time);
        return tempResult;
    }

    private static int leastBound;
    private static List<Integer> tempResult = new ArrayList<Integer>();

    private static void search(GeneralWorker w, Point location, List<Integer> candidates, List<Integer> finished, long time) {
        if (candidates.size() == 0 || finished.size() == w.getCapacity()) {
            // found a better result
            if (finished.size() > tempResult.size()) {
                tempResult.clear();
                tempResult.addAll(finished);
                // update lower bound
                if (leastBound < tempResult.size()) {
                    leastBound = tempResult.size();
                }
            }
            return;
        }

        List<Node> newCandidates = new ArrayList<Node>();
        // calculate upper bound for each node in the next level
        // logger.debug("[calculate upper bound][depth " + finished.size() + "]");
        for (Integer id : candidates) {
            Node temp = calcUpperBound(id, location, candidates, time);
            if (temp != null) {
                newCandidates.add(temp);
            }
        }

        // calculate lower bound for each candidate
        // logger.debug("[calculate lower bound]");
        for (Node tu : newCandidates) {
            int lower = WorkerSelectHeuriApproxi
                    .leastExpireTime(w.getCapacity() - finished.size() - 1, tu.location, tu.candidates, tu.time, taskMap)
                    .size();
            if (lower + finished.size() + 1 > leastBound) {
                leastBound = lower + finished.size() + 1;
            }
        }

        // sort nodes on upper bound decreasing
        Collections.sort(newCandidates, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                if (o1.candidates.size() < o2.candidates.size()) {
                    return -1;
                } else if (o1.candidates.size() > o2.candidates.size()) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        // search the nodes in the decreasing order of their upper bound
        // prune if upper bound is less than the least bound
        for (Node tu : newCandidates) {
            if (tu.candidates.size() + finished.size() + 1 >= leastBound) {
                finished.add(tu.id);
                search(w, tu.location, tu.candidates, finished, tu.time);
                finished.remove(finished.size() - 1);
            }
        }
    }

    // calculate the upper bound of candidate nodes after node "id" is chosen
    private static Node calcUpperBound(int id, Point p, List<Integer> candidates, long time) {
        GeneralTask t = taskMap.get(id);
        long expectTime = WorkerSelectDP.travelCost(t.location, p) + time;
        if (expectTime <= t.expiryTime) {
            List<Integer> upperBound = new ArrayList<Integer>();
            upperBound.addAll(candidates);
            for (Integer toRemove : candidates) {
                GeneralTask t2 = taskMap.get(toRemove);
                if (toRemove.equals(id)
                        || WorkerSelectDP.travelCost(t.location, t2.location) + expectTime > t2.expiryTime) {
                    upperBound.remove(toRemove);
                }
            }
            Node temp = new Node();
            temp.id = id;
            temp.candidates = upperBound;
            temp.location = t.location;
            temp.time = expectTime;
            return temp;
        } else {
            return null;
        }
    }

    public static Node calcUpperBound(int id, Point p, List<Integer> candidates, long time, Map<Integer, GeneralTask> tMap) {
        taskMap = tMap;
        return calcUpperBound(id, p, candidates, time);
    }

    public static class Node {
        // chosen id
        Integer id = null;
        List<Integer> candidates;
        long time;
        Point location;
    }
}
