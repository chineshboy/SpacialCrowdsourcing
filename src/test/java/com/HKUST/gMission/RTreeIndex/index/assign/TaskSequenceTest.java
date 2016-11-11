package com.HKUST.gMission.RTreeIndex.index.assign;

import com.HKUST.gMission.SpacialCroudsourcing.assign.WorkerSelectDP;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.TaskSequence;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jianxun on 5/23/16.
 */
public class TaskSequenceTest {
    @Test
    public void test1() {
        List<Integer> l1 = new ArrayList<Integer>();
        List<Integer> l2 = new ArrayList<Integer>();
        for (int i = 0; i < 5; i++) {
            l1.add(i);
            l2.add(4 - i);
        }
        TaskSequence ts1 = new TaskSequence(l1);
        TaskSequence ts2 = new TaskSequence(l2);
        System.out.println(ts1.equals(ts2));
        Assert.assertEquals(ts1.equals(ts2), true);
    }

    @Test
    public void test2() {
        List<Integer> l1 = new ArrayList<Integer>();
        List<Integer> l2 = new ArrayList<Integer>();
        l1.add(1);
        l1.add(2);
        l1.add(3);
        l2.add(3);
        l2.add(2);
        l2.add(4);
        List<Integer> l3 = new ArrayList<Integer>();
        l3.add(1);
        l3.add(2);
        l3.add(3);
        l3.add(4);
        TaskSequence ts3 = new TaskSequence(l3);
        TaskSequence merged = new TaskSequence(WorkerSelectDP.merge(l1, l2));
        Assert.assertEquals(ts3.equals(merged), true);
    }
}
