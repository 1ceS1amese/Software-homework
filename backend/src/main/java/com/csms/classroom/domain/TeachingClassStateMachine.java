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
            throw new BizException(ErrorCode.PARAM_ERROR, "教学班状态缺失");
        }

        switch (action) {
            case PUBLISH:
                if (from != TeachingClassStatus.DRAFT) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "只有草稿教学班可以发布");
                }
                if (teachingClass.getCapacity() == null || teachingClass.getCapacity() <= 0) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "发布时容量必须大于0");
                }
                if (schedules == null || schedules.isEmpty()) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "发布前至少安排一个时段");
                }
                break;
            case CLOSE:
                if (from != TeachingClassStatus.PUBLISHED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "只有已发布教学班可以关闭");
                }
                break;
            case CANCEL:
                if (from != TeachingClassStatus.DRAFT && from != TeachingClassStatus.PUBLISHED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "只有草稿或已发布教学班可以取消");
                }
                break;
            case ROLLBACK_DRAFT:
                if (from != TeachingClassStatus.PUBLISHED) {
                    throw new BizException(ErrorCode.TC_STATUS_INVALID, "只有已发布教学班可以退回草稿");
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
