package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.AssignmentHistory;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralWorker;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoCrowdNNP {

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, SpatialIndex idx,
                                       AssignmentHistory history) {
        // create a map for workers
        Map<Integer, GeneralWorker> workerMap = new HashMap<Integer, GeneralWorker>();
        for (GeneralWorker w : workers) {
            workerMap.put(w.id, w);
        }

        // assign new ids to tasks and workers,
        // maintain a map between new ids and old ids,
        // src has id 0 and dst has the maximum id
        Map<Integer, Integer> taskNewId = new HashMap<Integer, Integer>();
        Map<Integer, Integer> workerNewId = new HashMap<Integer, Integer>();
        int currId = 0;
        for (GeneralTask t : tasks) {
            currId++;
            taskNewId.put(t.id, currId);
        }
        for (GeneralWorker w : workers) {
            currId++;
            workerNewId.put(w.id, currId);
        }
        currId++;
        int dstId = currId;

        // create the capacity and cost array, in this strategy, all costs are 1
        int[][] capacity = new int[currId + 1][currId + 1];
        double[][] cost = new double[currId + 1][currId + 1];
        for (GeneralTask t : tasks) {
            int newTaskId = taskNewId.get(t.id);
            capacity[0][newTaskId] = t.getRequirement();
            cost[0][newTaskId] = 1.0;
            Rectangle rect = new Rectangle(t.longitude, t.latitude, t.longitude, t.latitude);
            List<Integer> relatedWorkerIds = idx.intersects(rect);
            for (int wId : relatedWorkerIds) {
                if (workerNewId.containsKey(wId) && !history.has(wId, t.id)) {
                    capacity[newTaskId][workerNewId.get(wId)] = 1;
                    GeneralWorker w = workerMap.get(wId);
                    cost[newTaskId][workerNewId.get(wId)] = distance(t.longitude, t.latitude, w.longitude, w.latitude);
                }
            }
        }
        for (GeneralWorker w : workers) {
            capacity[workerNewId.get(w.id)][dstId] = w.getCapacity();
            cost[workerNewId.get(w.id)][dstId] = 1.0;
        }

        // use max flow to find the maximum assignment
        MinCostMaxFlow maxFlow = new MinCostMaxFlow();
        maxFlow.getMaxFlow(capacity, cost, 0, dstId);

        List<WTMatch> result = new ArrayList<WTMatch>();

        for (GeneralTask t : tasks) {
            for (GeneralWorker w : workers) {
                if (maxFlow.hasFlow(taskNewId.get(t.id), workerNewId.get(w.id))) {
                    result.add(new WTMatch(t.id, w.id));
                }
            }
        }

        return result;
    }

    public static double distance(double lon1, double lat1, double lon2, double lat2) {
        return Math.sqrt((lon1 - lon2) * (lon1 - lon2) + (lat1 - lat2) * (lat1 - lat2));
    }
}
