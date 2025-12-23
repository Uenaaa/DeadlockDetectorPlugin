package com.deadlock.detector.detector;

import com.deadlock.detector.model.GraphNode;

import java.util.List;

public class DeadlockDetectionResult {
    private final boolean hasDeadlock;
    private final List<List<GraphNode>> cycles;

    public DeadlockDetectionResult(boolean hasDeadlock, List<List<GraphNode>> cycles) {
        this.hasDeadlock = hasDeadlock;
        this.cycles = cycles;
    }

    public boolean isHasDeadlock() {
        return hasDeadlock;
    }

    public List<List<GraphNode>> getCycles() {
        return cycles;
    }
}