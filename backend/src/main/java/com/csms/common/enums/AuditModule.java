package com.csms.common.enums;

public enum AuditModule {
    AUTH, BASE, CLASS, ENROLL, GRADE, USER;
    public String code() { return this.name(); }
}
