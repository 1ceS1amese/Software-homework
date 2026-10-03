package com.csms.common.enums;

public enum ConfigValueType {
    STRING, NUMBER, BOOL, JSON;
    public String code() { return this.name(); }
}
