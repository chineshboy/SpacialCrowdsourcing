package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

public class WTMatch {

    public int taskId;

    public int workerId;

    public WTMatch(int tId, int wId) {
        this.taskId = tId;
        this.workerId = wId;
    }

    public WTMatch(int tId, long wId) {
        this.taskId = tId;
        this.workerId = new Long(wId).intValue();
    }

    @Override
    public String toString() {
        return "{taskId: " + taskId + ", workerId: " + workerId + "}";
    }
}
