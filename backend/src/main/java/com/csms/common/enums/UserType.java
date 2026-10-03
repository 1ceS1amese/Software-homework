package com.csms.common.enums;

public enum UserType {
    STUDENT, TEACHER, ADMIN;
    public String code() { return this.name(); }
}
