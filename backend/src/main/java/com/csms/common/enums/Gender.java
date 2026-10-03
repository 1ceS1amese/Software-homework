package com.csms.common.enums;

public enum Gender {
    MALE, FEMALE, UNKNOWN;
    public String code() { return this.name(); }
}
