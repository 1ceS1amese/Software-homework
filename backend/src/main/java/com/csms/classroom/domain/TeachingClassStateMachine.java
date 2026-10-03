package com.csms.classroom.domain;

import com.csms.classroom.entity.ClassSchedule;
import com.csms.classroom.entity.TeachingClass;
import com.csms.common.enums.TeachingClassStatus;
import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TeachingClassStateMachine {

    public enum Action {
        PUBLISH, CLOSE, CANCEL, ROLLBACK_DRAFT
    }

    public boolean canTransit(TeachingClass teachingClass, List<ClassSchedule> schedules, Action action) {
        try {
            assertTransit(teachingClass, schedules, action);
            return true;
        } catch (BizException e) {
            return false;
        }
    }

    public void assertTransit(TeachingClass teachingClass, List<ClassSchedule> schedules, Action action) {
        TeachingClassStatus from = teachingClass.getStatus();
        if (from == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Current status is null");
        }

        switch (action) {
            case PUBLISH:
                if (from != TeachingClassStatus.DRAFT) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "Only DRAFT class can be published");
                }
                if (teachingClass.getCapacity() == null || teachingClass.getCapacity() <= 0) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "Capacity must be > 0 to publish");
                }
                if (schedules == null || schedules.isEmpty()) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "At least 1 schedule is required to publish");
                }
                break;
            case CLOSE:
                if (from != TeachingClassStatus.PUBLISHED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "Only PUBLISHED class can be closed");
                }
                break;
            case CANCEL:
                if (from == TeachingClassStatus.CANCELLED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "Class is already cancelled");
                }
                break;
            case ROLLBACK_DRAFT:
                if (from != TeachingClassStatus.PUBLISHED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "Only PUBLISHED class can be rolled back to DRAFT");
                }
                break;
            default:
                throw new BizException(ErrorCode.PARAM_ERROR, "Unknown action");
        }
    }
    
    public TeachingClassStatus getTargetStatus(Action action) {
        switch (action) {
            case PUBLISH: return TeachingClassStatus.PUBLISHED;
            case CLOSE: return TeachingClassStatus.CLOSED;
            case CANCEL: return TeachingClassStatus.CANCELLED;
            case ROLLBACK_DRAFT: return TeachingClassStatus.DRAFT;
            default: throw new IllegalArgumentException("Unknown action");
        }
    }
}
