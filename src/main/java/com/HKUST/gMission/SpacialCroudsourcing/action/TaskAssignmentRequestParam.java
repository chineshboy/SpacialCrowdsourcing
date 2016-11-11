package com.HKUST.gMission.SpacialCroudsourcing.action;

import com.HKUST.gMission.SpacialCroudsourcing.assign.module.GeneralTask;
import com.HKUST.gMission.SpacialCroudsourcing.assign.module.RegionWorker;

import java.util.List;

public class TaskAssignmentRequestParam {
    public List<GeneralTask> tasks;
    public List<RegionWorker> workers;
}
