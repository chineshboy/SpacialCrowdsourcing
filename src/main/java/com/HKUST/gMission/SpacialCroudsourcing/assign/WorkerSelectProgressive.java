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
 * Created by jianxun on 16/5/15.
 */
public class WorkerSelectProgressive {
    private static Map<Integer, GeneralTask> taskMap;

    public static Logger logger = LogManager.getLogger();

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, SpatialIndex idx,
                                       AssignmentHistory history, long currentTime) {
        taskMap = new HashMap<Integer, GeneralTask>();
        for (GeneralTask t : tasks) {
            taskMap.put(t.id, t);
        }
        AssignmentHistory tempHistory = new AssignmentHistory();
        List<WTMatch> result = new ArrayList<WTMatch>();
        for (GeneralWorker w : workers) {
            List<Integer> taskIds = idx.intersects(w.region);
            Iterator<Integer> iter = taskIds.iterator();
            while (iter.hasNext()) {
                Integer tId = iter.next();
                GeneralTask t = taskMap.get(tId);
                if (history.has(w.id, tId) || t.getRequirement() <= tempHistory.getAssignedNumOfTask(tId)
                        || currentTime + WorkerSelectDP.travelCost(t.location, w.location) > t.expiryTime) {
                    iter.remove();
                }
            }
            logger.debug("[assign worker][task num " + taskIds.size() + "][" + taskIds + "]");
            // firstly use Most Promising heuristic to get an answer
            List<Integer> tempResult = WorkerSelectHeuriApproxi.
                    mostPromising(w.getCapacity(), w.location, taskIds, currentTime, taskMap);
            logger.debug("[result num: " + tempResult.size() + "][" + tempResult + "]");
            // calculate the worker's stat when finished all the tasks
            Point location = w.location;
            long time = currentTime;
            for (Integer tid : tempResult) {
                // add to result
                result.add(new WTMatch(tid, w.id));
                tempHistory.add(w.id, tid);
                // calculate stat
                time += WorkerSelectDP.travelCost(location, taskMap.get(tid).location);
                location = taskMap.get(tid).location;
                // add to history (for duplicate checking later) and remove this task
                taskIds.remove(tid);
                history.add(w.id, tid);
            }
            // then use Branch and Bound to get more tasks
            // should set "finished" to tempResult for capacity checking
            logger.debug("[append worker][task num " + taskIds.size() + "]");
            List<Integer> appended = WorkerSelectBB.search(w, location, taskIds, tempResult, time, taskMap);
            logger.debug("[append result " + appended.size() + "]");
            for (Integer id : appended) {
                // check duplicate results (added from tempResult before)
                if (!history.has(w.id, id)) {
                    result.add(new WTMatch(id, w.id));
                    tempHistory.add(w.id, id);
                }
            }
        }
        return result;
    }
}
