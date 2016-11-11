package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoTruCrowdLO {

    private static Map<Integer, List<List<Integer>>> possibleSet;

    private static Map<Integer, List<Integer>> assigned;

    private static Map<Integer, Integer> workload;

    private static Map<Integer, GeneralWorker> workerMap;

    private static AssignmentHistory history;

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, AssignmentHistory history) {
        possibleSet = new HashMap<Integer, List<List<Integer>>>();
        assigned = new HashMap<Integer, List<Integer>>();
        workload = new HashMap<Integer, Integer>();
        workerMap = new HashMap<Integer, GeneralWorker>();
        GeoTruCrowdLO.history = history;
        for (GeneralWorker w : workers) {
            workerMap.put(w.id, w);
            workload.put(w.id, 0);
        }

        // get a greedy assignment
        List<WTMatch> matches = GeoTruCrowdGreedy.assign(tasks, workers, history);
        for (WTMatch m : matches) {
            if (!assigned.containsKey(m.taskId)) {
                assigned.put(m.taskId, new ArrayList<Integer>());
            }
            assigned.get(m.taskId).add(m.workerId);
            int load = workload.get(m.workerId);
            workload.put(m.workerId, load + 1);
        }

        for (GeneralTask t : tasks) {
            if (assigned.containsKey(t.id) && assigned.get(t.id).size() > 0) {
                List<Integer> list = assigned.get(t.id);
                for (int wid : list) {
                    int wl = workload.get(wid);
                    workload.put(wid, wl - 1);
                }

                // find possible set for every task
                for (GeneralTask task : tasks) {
                    possibleSet.put(task.id, new ArrayList<List<Integer>>());
                    if (task.assignedCount == 0) {
                        Point location = new Point(task.longitude, task.latitude);
                        List<GeneralWorker> availableWorkers = new ArrayList<GeneralWorker>();
                        for (GeneralWorker w : workers) {
                            if (w.getCapacity() > workload.get(w.id)
                                    && w.region.contains(location)
                                    && !history.has(w.id, t.id)) {
                                availableWorkers.add(w);
                            }
                        }
                        getPossibleSet(task, location, availableWorkers, 0, new ArrayList<Integer>(),
                                new ArrayList<Double>());
                    }
                }
                // find available tasks (not assigned)
                List<GeneralTask> available = new ArrayList<GeneralTask>();
                for (GeneralTask task : tasks) {
                    if (task.id != t.id && !assigned.containsKey(task.id) && task.assignedCount == 0) {
                        available.add(task);
                    }
                }

                Map<Integer, List<Integer>> alternate = new HashMap<Integer, List<Integer>>();
                // see how many tasks can be assigned if do not assign this task
                // logger.info("find most assign");
                findMostAssign(available, 0, alternate, new HashMap<Integer, List<Integer>>(), new HashMap<Integer, Integer>());
                // logger.info("find most assign finished");
                if (alternate.size() > 1) {
                    logger.info("[replace task " + t.id + " with " + alternate.keySet() + "]");
                    // replace t with tasks in alternate
                    assigned.remove(t.id);
                    for (int tid : alternate.keySet()) {
                        assigned.put(tid, alternate.get(tid));
                    }
                    // update workload
                    for (int tid : alternate.keySet()) {
                        for (int wid : alternate.get(tid)) {
                            if (workload.containsKey(wid)) {
                                workload.put(wid, workload.get(wid) + 1);
                            } else {
                                workload.put(wid, 1);
                            }
                        }
                    }
                } else {
                    // recover the workload
                    for (int wid : list) {
                        int wl = workload.get(wid);
                        workload.put(wid, wl + 1);
                    }
                }
            }
        }

        List<WTMatch> result = new ArrayList<WTMatch>();
        for (int tid : assigned.keySet()) {
            for (int wid : assigned.get(tid)) {
                WTMatch match = new WTMatch(tid, wid);
                result.add(match);
            }
        }

        return result;
    }

    private static void getPossibleSet(GeneralTask t, Point taskLocation, List<GeneralWorker> workers, int workerIdx,
                                       List<Integer> current, List<Double> reputations) {
        // prune
        if (current.size() + workers.size() - workerIdx - 1 < t.getRequirement()) {
            return;
        }
        // cut on depth 20 to avoid too many choices
        if (workerIdx > 20) {
            return;
        }
        // try add this worker
            current.add(workers.get(workerIdx).id);
            reputations.add(workers.get(workerIdx).reliability);
            // if the task can be done, add this result and return to the previous call
            // otherwise continue adding worker
            if (current.size() >= t.getRequirement() && GeoTruCrowdGreedy.ars(reputations, 0, 1, 0) >= t.confidence) {
                List<Integer> temp = new ArrayList<Integer>();
                temp.addAll(current);
                possibleSet.get(t.id).add(temp);
            } else if (workerIdx < workers.size() - 1) {
                getPossibleSet(t, taskLocation, workers, workerIdx + 1, current, reputations);
            }
            current.remove(current.size() - 1);
            reputations.remove(reputations.size() - 1);
        // try not add this worker
        if (workerIdx < workers.size() - 1) {
            getPossibleSet(t, taskLocation, workers, workerIdx + 1, current, reputations);
        }
    }

    private static void findMostAssign(List<GeneralTask> tasks, int taskIdx, Map<Integer, List<Integer>> result,
                                       Map<Integer, List<Integer>> current, Map<Integer, Integer> currentWorkLoad) {
        if (taskIdx == tasks.size()) {
            if (current.size() > result.size()) {
                result.clear();
                for (int tid : current.keySet()) {
                    result.put(tid, current.get(tid));
                }
            }
            return;
        }
        // prune
        if (current.size() + tasks.size() - taskIdx - 1 < result.size()) {
            return;
        }
        // try this task
        GeneralTask t = tasks.get(taskIdx);
        List<List<Integer>> sets = possibleSet.get(t.id);
        // choose one possible choice
        for (List<Integer> choice : sets) {
            boolean valid = true;
            for (int wid : choice) {
                int temp = workload.get(wid);
                if (currentWorkLoad.containsKey(wid)) {
                    temp += currentWorkLoad.get(wid);
                }
                if (temp >= workerMap.get(wid).getCapacity()) {
                    valid = false;
                }
            }
            if (valid) {
                // add to current
                current.put(t.id, choice);
                for (int wid : choice) {
                    if (currentWorkLoad.containsKey(wid)) {
                        currentWorkLoad.put(wid, currentWorkLoad.get(wid) + 1);
                    } else {
                        currentWorkLoad.put(wid, 1);
                    }
                }
                findMostAssign(tasks, taskIdx + 1, result, current, currentWorkLoad);
                // remove from current
                current.remove(t.id);
                for (int wid : choice) {
                    currentWorkLoad.put(wid, currentWorkLoad.get(wid) - 1);
                }
            }
        }
        findMostAssign(tasks, taskIdx + 1, result, current, currentWorkLoad);
    }
}
