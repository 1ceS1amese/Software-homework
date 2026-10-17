package com.csms.classroom.domain;
import com.csms.classroom.entity.*;
import com.csms.common.enums.TeachingClassStatus;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class TeachingClassStateMachineTest {
    private final TeachingClassStateMachine machine=new TeachingClassStateMachine();
    private TeachingClass row(TeachingClassStatus status) { var row=new TeachingClass();row.setStatus(status);row.setCapacity(2);return row; }
    @Test void publicationRequiresCapacityAndSchedule() {
        var draft=row(TeachingClassStatus.DRAFT);
        assertFalse(machine.canTransit(draft,List.of(),TeachingClassStateMachine.Action.PUBLISH));
        assertTrue(machine.canTransit(draft,List.of(new ClassSchedule()),TeachingClassStateMachine.Action.PUBLISH));
        draft.setCapacity(0);assertFalse(machine.canTransit(draft,List.of(new ClassSchedule()),TeachingClassStateMachine.Action.PUBLISH));
    }
    @Test void terminalStatesCannotBeCancelledOrPublishedAgain() {
        for(var status:List.of(TeachingClassStatus.CLOSED,TeachingClassStatus.CANCELLED)) {
            assertFalse(machine.canTransit(row(status),List.of(new ClassSchedule()),TeachingClassStateMachine.Action.CANCEL));
            assertFalse(machine.canTransit(row(status),List.of(new ClassSchedule()),TeachingClassStateMachine.Action.PUBLISH));
        }
    }
    @Test void closingAndRollbackRequirePublishedState() {
        assertTrue(machine.canTransit(row(TeachingClassStatus.PUBLISHED),List.of(),TeachingClassStateMachine.Action.CLOSE));
        assertTrue(machine.canTransit(row(TeachingClassStatus.PUBLISHED),List.of(),TeachingClassStateMachine.Action.ROLLBACK_DRAFT));
        assertFalse(machine.canTransit(row(TeachingClassStatus.DRAFT),List.of(),TeachingClassStateMachine.Action.CLOSE));
    }
}
