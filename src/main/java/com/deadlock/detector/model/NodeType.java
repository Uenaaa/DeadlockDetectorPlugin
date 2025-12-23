package com.deadlock.detector.model;

public enum NodeType {
    PROCESS("PROCESS"),
    RESOURCE("RESOURCE");

    private final String type;

    NodeType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}