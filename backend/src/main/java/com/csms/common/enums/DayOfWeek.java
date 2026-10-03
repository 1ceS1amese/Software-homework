package com.csms.common.enums;

public enum DayOfWeek {
    MON(1), TUE(2), WED(3), THU(4), FRI(5), SAT(6), SUN(7);
    
    private final int value;
    
    DayOfWeek(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public String code() {
        return String.valueOf(value);
    }
}
