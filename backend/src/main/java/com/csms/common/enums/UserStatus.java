package com.csms.common.enums;

public enum UserStatus {
    ACTIVE, DISABLED;
    public String code() { return this.name(); }
}
