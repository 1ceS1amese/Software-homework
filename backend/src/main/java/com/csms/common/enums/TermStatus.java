package com.csms.common.enums;

public enum TermStatus {
    PLANNED, ENROLLING, RUNNING, CLOSED;
    public String code() { return this.name(); }
}
