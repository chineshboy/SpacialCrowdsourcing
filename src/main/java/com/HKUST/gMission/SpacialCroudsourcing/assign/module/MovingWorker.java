package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

public class MovingWorker extends GeneralWorker {

    // the worker is heading in angle [minDirection, maxDirection], which forms an angle in [0, 2π)
    public double minDirection;
    public double maxDirection;
    public double velocity;
    public double reliability;
}
