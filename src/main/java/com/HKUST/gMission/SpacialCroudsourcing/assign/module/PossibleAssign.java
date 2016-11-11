package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianxun on 16/4/28.
 */
public class PossibleAssign implements Comparable<PossibleAssign>{
    public GeneralTask task;
    public List<GeneralWorker> workers = new ArrayList<GeneralWorker>();

    public PossibleAssign(GeneralTask t, List<GeneralWorker> workers) {
        this.task = t;
        for (GeneralWorker w : workers) {
            this.workers.add(w);
        }
    }

    private double aggregateDis() {
        double dis = 0;
        for (GeneralWorker w : this.workers) {
            dis += (this.task.longitude - w.longitude) * (this.task.longitude - w.longitude) +
                    (this.task.latitude - w.latitude) * (this.task.latitude - w.latitude);
        }
        return dis;
    }

    @Override
    public int compareTo(PossibleAssign o) {
        if (this.workers.size() < o.workers.size()) {
            return -1;
        } else if (this.workers.size() > o.workers.size()) {
            return 1;
        } else if (this.aggregateDis() < o.aggregateDis()) {
            return -1;
        } else if (this.aggregateDis() > o.aggregateDis()) {
            return 1;
        } else {
            return 0;
        }
    }
}
