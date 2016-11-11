package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Point;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * dynamic programming
 * Created by jianxun on 16/5/2.
 */
public class WorkerSelectDP {

    private static Map<Integer, GeneralTask> taskMap;
    private static long currentTime;
    private static List<TaskSequence> results;

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, SpatialIndex idx,
                                AssignmentHistory history, long cTime) {
        currentTime = cTime;
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
            List<Integer> sequence = findMaxSequence(w, taskIds);
            for (Integer tid : sequence) {
                result.add(new WTMatch(tid, w.id));
                tempHistory.add(w.id, tid);
            }
        }

        return result;
    }

    // similar to apriori
    private static List<Integer> findMaxSequence(GeneralWorker worker, List<Integer> taskIds) {
        logger.debug("[find max sequence][capacity " + worker.getCapacity() + "][task num " + taskIds.size() + "]");
        results = new ArrayList<TaskSequence>();
        // starting from one task sets
        for (int tid : taskIds) {
            GeneralTask task = taskMap.get(tid);
            long finishTime = currentTime + travelCost(task.location, worker.location);
            if (finishTime <= task.expiryTime) {
                List<Integer> temp = new ArrayList<Integer>();
                temp.add(tid);
                TaskSequence ts = new TaskSequence(temp);
                ts.finishedTime = finishTime;
                results.add(ts);
            }
        }

        // merge shorter sequences to longer sequences
        int head = 0;
        int tail = results.size();
        while (head < tail) {
            if (results.get(head).size() >= worker.getCapacity()) {
                break;
            }
            // logger.info("[merge " + (tail - head) + " sequences of length " + results.get(head).size() + "]");
            for (int i = head; i < tail; i++) {
                TaskSequence ith = results.get(i);
                for (int j = i + 1; j < tail; j++) {
                    List<Integer> merged = merge(ith.sequence, results.get(j).sequence);
                    if (merged.size() == 0) {
                        continue;
                    }
                    // check for duplicate
                    TaskSequence temp = new TaskSequence(merged);
                    boolean contained = false;
//                    for (int k = results.size() - 1; k >= 0 && results.get(k).size() == temp.size(); k--) {
//                        if (results.get(k).equals(temp)) {
//                            contained = true;
//                        }
//                    }
                    if (!contained) {
                        // choose the last task to be finished and test it
                        for (Integer last : merged) {
                            TaskSequence checked = check(worker, merged, last);
                            if (checked != null) {
                                results.add(checked);
                            }
                        }
                    }
                    if (results.size() > 0 && results.get(results.size() - 1).size() >= worker.getCapacity()) {
                        break;
                    }
                }
                if (results.size() > 0 && results.get(results.size() - 1).size() >= worker.getCapacity()) {
                    break;
                }
            }
            head = tail;
            tail = results.size();
            if (results.size() > 0 && results.get(results.size() - 1).size() >= worker.getCapacity()) {
                break;
            }
        }
        tail = results.size();
        if (tail > 0) {
            logger.debug("[sequence length " + results.get(tail - 1).sequence.size() + "]");
            return results.get(tail - 1).sequence;
        } else {
            return new ArrayList<Integer>();
        }
    }

    public static List<Integer> merge(List<Integer> seq1, List<Integer> seq2) {
        List<Integer> result = new ArrayList<Integer>();
        if (seq1.size() != seq2.size()) {
            return result;
        }
        int diff = 0;
        for (Integer id : seq1) {
            if (!seq2.contains(id)) {
                diff += 1;
            }
        }
        if (diff == 1) {
            for (Integer id : seq1) {
                result.add(id);
            }
            for (Integer id : seq2) {
                if (!result.contains(id)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    // check if the worker can finish all the tasks with "last" as the last one
    private static TaskSequence check(GeneralWorker worker, List<Integer> taskIds, int last) {
        if (worker.getCapacity() < taskIds.size()) {
            return null;
        }
        List<Integer> seq = new ArrayList<Integer>();
        for (Integer id : taskIds) {
            if (id != last) {
                seq.add(id);
            }
        }
        seq.add(last);
        GeneralTask lastTask = taskMap.get(last);
        long minTime = Integer.MAX_VALUE;
        TaskSequence result = new TaskSequence(seq);
        // choose previous results that is parent of this sequence
        // "parent" means is a prefix
        for (int i = results.size() - 1; i >= 0 && results.get(i).size() >= seq.size() - 1; i--) {
            TaskSequence ts = results.get(i);
            if (ts.size() == seq.size() - 1 && result.isPrefix(ts)) {
                long expectTime = travelCost(taskMap.get(ts.last()).location, lastTask.location) + ts.finishedTime;
                if (expectTime <= lastTask.expiryTime && expectTime < minTime) {
                    minTime = expectTime;
                    List<Integer> ids = new ArrayList<Integer>();
                    ids.addAll(ts.sequence);
                    ids.add(last);
                    result.sequence = ids;
                    result.finishedTime = expectTime;
                }
            }
        }
        if (minTime < Integer.MAX_VALUE) {
            return result;
        }
        return null;
    }

    public static int travelCost(Point s, Point t) {
        return (int) Math.ceil(s.distance(t));
    }
}
