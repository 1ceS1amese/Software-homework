package com.csms.common.enums;

public enum LoginResult {
    SUCCESS, BAD_PASSWORD, USER_NOT_FOUND, DISABLED, LOCKED;
    public String code() { return this.name(); }
}
