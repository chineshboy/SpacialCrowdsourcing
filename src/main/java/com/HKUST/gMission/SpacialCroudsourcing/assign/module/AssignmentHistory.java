package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

/**
 * Created by jianxun on 16/4/9.
 */
public class AssignmentHistory {
    private HashSet<Long> history;
    private static final long maxWid = 1000000;
    private HashMap<Integer, List<Double>> reliabilityAssigned;
    // the number of assigned workers of a task
    private HashMap<Integer, Integer> assignedNumberOfTask;
    private HashMap<Integer, Integer> assignedNumberOfWorker;

    public AssignmentHistory() {
        this.history = new HashSet<Long>();
        this.reliabilityAssigned = new HashMap<Integer, List<Double>>();
        this.assignedNumberOfTask = new HashMap<Integer, Integer>();
        this.assignedNumberOfWorker = new HashMap<Integer, Integer>();
    }

    public AssignmentHistory(JdbcTemplate jdbcTemplate) {
        String sql = "select attachment, receiver_id, reliability" +
                " from gmission_hkust.message join gmission_hkust.worker_detail" +
                " on receiver_id = worker_detail.id";
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);

        this.history = new HashSet<Long>();
        this.reliabilityAssigned = new HashMap<Integer, List<Double>>();
        for (Map<String, Object> row : result) {
            int tid = Integer.parseInt((String) row.get("attachment"));
            int wid = (Integer) row.get("receiver_id");
            double r = (Double) row.get("reliability");
            if (!this.reliabilityAssigned.containsKey(tid)) {
                this.reliabilityAssigned.put(tid, new ArrayList<Double>());
            }
            this.reliabilityAssigned.get(tid).add(r);
            this.history.add(tid * maxWid + wid);
        }
    }

    public void add(int wid, int tid) {
        this.history.add(tid * maxWid + wid);
        if (this.assignedNumberOfTask.containsKey(tid)) {
            this.assignedNumberOfTask.put(tid, this.assignedNumberOfTask.get(tid) + 1);
        } else {
            this.assignedNumberOfTask.put(tid, 1);
        }
        if (this.assignedNumberOfWorker.containsKey(wid)) {
            this.assignedNumberOfWorker.put(wid, this.assignedNumberOfWorker.get(wid) + 1);
        } else {
            this.assignedNumberOfWorker.put(wid, 1);
        }
    }

    public boolean has(int wid, int tid) {
        return this.history.contains(tid * maxWid + wid);
    }

    public void remove(int wid, int tid) {
        this.history.remove(tid * maxWid + wid);
    }

    public List<Double> getAssignedReliability(int tid) {
        if (this.reliabilityAssigned.containsKey(tid)) {
            return this.reliabilityAssigned.get(tid);
        } else {
            return new ArrayList<Double>();
        }
    }

    public int getAssignedNumOfTask(int tid) {
        if (this.assignedNumberOfTask.containsKey(tid)) {
            return this.assignedNumberOfTask.get(tid);
        } else {
            return 0;
        }
    }

    public int getAssignedNumOfWorker(int wid) {
        if (this.assignedNumberOfWorker.containsKey(wid)) {
            return this.assignedNumberOfWorker.get(wid);
        } else {
            return 0;
        }
    }
}
