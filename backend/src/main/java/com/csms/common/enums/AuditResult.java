package com.csms.common.enums;

public enum AuditResult {
    SUCCESS, FAIL;
    public String code() { return this.name(); }
}
