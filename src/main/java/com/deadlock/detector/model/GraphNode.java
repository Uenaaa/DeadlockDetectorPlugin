package com.deadlock.detector.model;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    private final String id;
    private final NodeType type;
    private final List<GraphNode> outgoingEdges;
    private LockType lockType;

    public GraphNode(String id, NodeType type, LockType lockType) {
        this.id = id;
        this.type = type;
        this.lockType = lockType;
        this.outgoingEdges = new ArrayList<>();
    }

    public GraphNode(String id, NodeType type) {
        this(id, type, null);
    }

    public String getId() {
        return id;
    }

    public NodeType getType() {
        return type;
    }

    public LockType getLockType() {
        return lockType;
    }

    public List<GraphNode> getOutgoingEdges() {
        return outgoingEdges;
    }

    public void addEdge(GraphNode to) {
        outgoingEdges.add(to);
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }
}