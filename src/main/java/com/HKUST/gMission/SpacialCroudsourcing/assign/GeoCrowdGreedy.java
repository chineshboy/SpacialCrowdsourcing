package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.AssignmentHistory;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralWorker;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.WTMatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeoCrowdGreedy {

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, SpatialIndex idx,
                                       AssignmentHistory history) {
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
            // logger.info("[" + relatedWorkerIds.size() + " workers are related with task " + t.id + "]");
            for (int wId : relatedWorkerIds) {
                if (workerNewId.containsKey(wId) && !history.has(wId, t.id)) {
                    capacity[newTaskId][workerNewId.get(wId)] = 1;
                    cost[newTaskId][workerNewId.get(wId)] = 1.0;
                }
            }
        }
        for (GeneralWorker w : workers) {
            capacity[workerNewId.get(w.id)][dstId] = w.getCapacity();
            cost[workerNewId.get(w.id)][dstId] = 1.0;
        }

        // use max flow to find the maximum assignment
        MinCostMaxFlow maxFlow = new MinCostMaxFlow();
        double[] check = maxFlow.getMaxFlow(capacity, cost, 0, dstId);
        logger.info("[flow: " + check[0] + "][cost: " + check[1] + "]");

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

}
