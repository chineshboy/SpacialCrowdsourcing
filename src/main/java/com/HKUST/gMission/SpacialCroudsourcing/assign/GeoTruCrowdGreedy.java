package com.HKUST.gMission.SpacialCroudsourcing.assign;

import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;

import java.util.ArrayList;
import java.util.List;

public class GeoTruCrowdGreedy {

    public static List<WTMatch> assign(List<GeneralTask> tasks, List<GeneralWorker> workers, AssignmentHistory history) {
        int[] assignedNum = new int[workers.size()];

        List<WTMatch> matches = new ArrayList<WTMatch>();
        // from begin to end, assign each task to first k workers, so that
        // the workers satisfy the task's requirements
        // k should be minimal
        for (GeneralTask t : tasks) {
            if (t.assignedCount == 0) {
                List<Integer> workerSet = findSet(t, workers, assignedNum);
                for (Integer idx : workerSet) {
                    assignedNum[idx] += 1;
                    WTMatch match = new WTMatch(t.id, workers.get(idx).id);
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    public static List<Integer> findSet(GeneralTask t, List<GeneralWorker> workers, int[] assignedNum) {
        List<Integer> picked = new ArrayList<Integer>();
        List<Double> pickedRepu = new ArrayList<Double>();
        for (int i = 0; i < workers.size(); i++) {
            GeneralWorker w = workers.get(i);
            if (w.getCapacity() > assignedNum[i] && w.region.contains(t.location)) {
                picked.add(i);
                pickedRepu.add(w.reliability);
                if (picked.size() >= t.getRequirement() && t.confidence <= ars(pickedRepu, 0, 1, 0)) {
                    return picked;
                }
            }
        }
        picked.clear();
        return picked;
    }

    // the possibility that the majority of workers give the right answer
    public static double ars(List<Double> reputations, int level, double confidence, int negCount) {
        double answer = 0;
        if (level == reputations.size()) {
            return confidence;
        }
        // assume this worker gives a positive answer
        answer += ars(reputations, level + 1, confidence * reputations.get(level), negCount);
        // assume this worker gives a negative answer
        if (negCount < (reputations.size() - 1) / 2) {
            answer += ars(reputations, level + 1, confidence * (1 - reputations.get(level)), negCount + 1);
        }
        return answer;
    }
}
