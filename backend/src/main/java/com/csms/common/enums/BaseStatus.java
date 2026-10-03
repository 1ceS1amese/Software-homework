package com.csms.common.enums;

public enum BaseStatus {
    ACTIVE, DISABLED;
    public String code() { return this.name(); }
}
