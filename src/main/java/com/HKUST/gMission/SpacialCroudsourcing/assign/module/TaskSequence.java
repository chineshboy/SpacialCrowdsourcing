package com.HKUST.gMission.SpacialCroudsourcing.assign.module;

import java.util.List;

/**
 * Created by jianxun on 16/5/2.
 */
public class TaskSequence {
    public List<Integer> sequence;
    public long finishedTime;
    private int size;

    public TaskSequence(List<Integer> ids) {
        this.sequence = ids;
        this.size = ids.size();
    }

    public boolean contains(TaskSequence o) {
        for (Integer id : o.sequence) {
            if (!this.sequence.contains(id)) {
                return false;
            }
        }
        return true;
    }

    public boolean isPrefix(TaskSequence o) {
        for (int i = 0; i < this.size - 1; i++) {
            if (!o.sequence.contains(this.sequence.get(i))) {
                return false;
            }
        }
        return true;
    }

    public Integer last() {
        return this.sequence.get(this.size - 1);
    }

    public int size() {
        return this.size;
    }

    public boolean equals(TaskSequence t) {
        if (this.size != t.size) {
            return false;
        }
        for (int i = 0; i < this.size; i++) {
            if ((!t.sequence.contains(this.sequence.get(i))) || (!this.sequence.contains(t.sequence.get(i)))) {
                return false;
            }
        }
        return true;
    }
}
