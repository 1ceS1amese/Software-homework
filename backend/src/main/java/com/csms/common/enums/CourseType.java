package com.csms.common.enums;

public enum CourseType {
    REQUIRED, ELECTIVE, RESTRICTED;
    public String code() { return this.name(); }
}
