package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class GeoTruCrowdHGR {

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, AssignmentHistory history) {

        // least worker assigned heuristic
        // less confident tasks may need less workers
        // confidence has more influence than requiredAnswerCount, so put requiredAnswerCount as second condition
        Collections.sort(tasks, new Comparator<GeneralTask>() {
            public int compare(GeneralTask t1, GeneralTask t2) {
                if (t1.confidence > t2.confidence) {
                    return 1;
                } else if (t1.confidence < t2.confidence) {
                    return -1;
                } else if (t1.getRequirement() > t2.getRequirement()) {
                    return 1;
                } else if (t1.getRequirement() < t2.getRequirement()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });

        List<PossibleAssign> possibleAssigns = new ArrayList<PossibleAssign>();
        // from begin to end, assign each task to first k workers, so that
        // the workers satisfy the task's requirements
        // k should be minimal
        for (GeneralTask t : tasks) {
            if (t.assignedCount == 0) {
                upperBound = workers.size();
                tempAssigns.clear();
                current.clear();
                curReli.clear();
                logger.debug("[find possible set for task " + t.id + "][worker num " + workers.size() + "]");
                findSet(t, workers, 0);
//                logger.info("possible set size of task " + t.id + " is " + tempAssigns.size());
                for (PossibleAssign assign : tempAssigns) {
                    possibleAssigns.add(assign);
                }
            }
        }
        // sort possible set according to heuristic
        Collections.sort(possibleAssigns);

        List<WTMatch> matches = new ArrayList<WTMatch>();
        Set<Integer> assigned = new HashSet<Integer>();
        for (PossibleAssign assign : possibleAssigns) {
            if ((!assigned.contains(assign.task.id)) && check(assign)) {
                for (GeneralWorker w : assign.workers) {
                    w.assigned += 1;
                    WTMatch match = new WTMatch(assign.task.id, w.id);
                    matches.add(match);
                }
                assigned.add(assign.task.id);
            }
        }
        return matches;
    }

    private static int upperBound;
    private static List<GeneralWorker> current = new ArrayList<GeneralWorker>();
    private static List<Double> curReli = new ArrayList<Double>();
    private static List<PossibleAssign> tempAssigns = new ArrayList<PossibleAssign>();
    private static void findSet(GeneralTask t, List<GeneralWorker> workers, int workerIdx) {
        // see if this is a correct answer
        if (current.size() >= t.getRequirement() && t.confidence <= GeoTruCrowdGreedy.ars(curReli, 0, 1, 0)) {
            // drop larger answers
            if (current.size() < upperBound) {
                tempAssigns.clear();
                upperBound = current.size();
            }
            if (current.size() == upperBound) {
                tempAssigns.add(new PossibleAssign(t, current));
//                if (tempAssigns.size() % 1000 == 0) {
//                    logger.info("temp size " + tempAssigns.size() + ", upper bound " + upperBound + ", depth " + workerIdx);
//                }
            }
            // prune the answers dominated by this answer
            return;
        }
        if (workerIdx >= workers.size()) {
            return;
        }
        // prune larger answers
        if (current.size() >= upperBound) {
            return;
        }
        // try add this worker
        GeneralWorker w = workers.get(workerIdx);
        if (w.getCapacity() > 0 && w.region.contains(t.location)) {
            current.add(w);
            curReli.add(w.reliability);
            findSet(t, workers, workerIdx + 1);
            current.remove(current.size() - 1);
            curReli.remove(curReli.size() - 1);
        }
        // try not add this worker
        findSet(t, workers, workerIdx + 1);
    }

    private static boolean check(PossibleAssign assign) {
        for (GeneralWorker w : assign.workers) {
            if (w.getCapacity() == 0) {
                return false;
            }
        }
        return true;
    }
}
