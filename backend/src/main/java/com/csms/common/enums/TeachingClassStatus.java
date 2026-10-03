package com.csms.common.enums;

public enum TeachingClassStatus {
    DRAFT, PUBLISHED, CLOSED, CANCELLED;
    public String code() { return this.name(); }
}
