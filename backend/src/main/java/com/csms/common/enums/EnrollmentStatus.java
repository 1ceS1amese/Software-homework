package com.csms.common.enums;

public enum EnrollmentStatus {
    ENROLLED, WITHDRAWN, COMPLETED;
    public String code() { return this.name(); }
}
