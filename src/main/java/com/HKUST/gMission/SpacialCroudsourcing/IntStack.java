package com.HKUST.gMission.SpacialCroudsourcing;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class IntStack implements Serializable {
    private static final long serialVersionUID = -148238472936L;
    private List<Integer> stack = new ArrayList<Integer>();
    private int size = 0;

    public void push(Integer item) {
        stack.add(item);
        size++;
    }

    public Integer pop() {
        size--;
        return stack.remove(size);
    }

    public void clear() {
        size = 0;
        stack.clear();
    }

    public Integer peek() {
        return stack.get(size - 1);
    }

    public int size() {
        return size;
    }
}
