package com.HKUST.gMission.RTreeIndex.index.assign;

import java.util.ArrayList;
import java.util.List;

import com.HKUST.gMission.SpacialCroudsourcing.assign.module.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.HKUST.gMission.SpacialCroudsourcing.Rectangle;
import com.HKUST.gMission.SpacialCroudsourcing.SpatialIndex;
import com.HKUST.gMission.SpacialCroudsourcing.assign.GeoCrowdGreedy;
import com.HKUST.gMission.SpacialCroudsourcing.rtree.RTree;

public class GeoCrowdGreedyTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void test() {
        List<GeneralTask> tasks = new ArrayList<GeneralTask>();
        List<GeneralWorker> workers = new ArrayList<GeneralWorker>();
        SpatialIndex idx = new RTree();
        idx.init(null);
        int task_num = 3;
        int worker_num = 5;
        for (int i = 0; i < task_num; i++) {
            GeneralTask t = new GeneralTask();
            t.id = i;
            t.requiredAnswerCount = 3;
            t.latitude = i;
            t.longitude = i;
            tasks.add(t);
        }
        for (int i = 0; i < worker_num; i++) {
            GeneralWorker w = new GeneralWorker();
            w.id = i;
            w.capacity = 5;
            w.latitude = i;
            w.longitude = i;
            List<Double> loc1 = new ArrayList<Double>();
            loc1.add(i - 1.5);
            loc1.add(i - 1.5);
            List<Double> loc2 = new ArrayList<Double>();
            loc2.add(i + 1.5);
            loc2.add(i + 1.5);
            Rectangle rec = new Rectangle(loc1, loc2, 2);
            w.region = rec;
            workers.add(w);
            idx.add(rec, i);
        }

        AssignmentHistory history = new AssignmentHistory();

        history.add(0, 0);
        history.add(1, 0);
        history.add(0, 1);
        history.add(1, 1);
        history.add(2, 1);
        history.add(1, 2);
        history.add(2, 2);
        history.add(3, 2);
	
        List<WTMatch> match = GeoCrowdGreedy.assign(tasks, workers, idx, history);

        System.out.println(match);

        Assert.assertEquals(match.size(), 0);
    }

}
