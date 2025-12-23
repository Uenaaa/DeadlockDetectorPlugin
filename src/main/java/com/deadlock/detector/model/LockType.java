package com.deadlock.detector.model;


public enum LockType {
    SYNCHRONIZED("SYNCHRONIZED"),
    REENTRANT_LOCK("REENTRANT_LOCK"),
    READ_LOCK("READ_LOCK"),
    WRITE_LOCK("WRITE_LOCK"),
    LOCK_SUPPORT("LOCK_SUPPORT"),
    CLASS_LOCK("CLASS_LOCK");

    private final String type;

    LockType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}