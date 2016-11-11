package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

/**
 * Created by jianxun on 16/5/3.
 */
public class Tuple<T, K> {
    private T first;
    private K second;

    public Tuple(T f, K s) {
        this.first = f;
        this.second = s;
    }

    public T first() {
        return this.first;
    }

    public K second() {
        return this.second;
    }
}
