package com.csms.enroll.domain;

public interface CapacityGuard {
    boolean tryOccupy(Long teachingClassId, Long userId);
}
