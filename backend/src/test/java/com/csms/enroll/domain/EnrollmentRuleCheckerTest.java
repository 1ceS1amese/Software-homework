package com.csms.enroll.domain;

import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrollmentRuleCheckerTest {

    private final StubJdbcTemplate jdbc = new StubJdbcTemplate();
    private final EnrollmentRuleChecker rules = new EnrollmentRuleChecker(jdbc);

    @Test
    void duplicateEnrollmentIsRejected() {
        jdbc.duplicateCount = 1;
        BizException error = assertThrows(BizException.class, () -> rules.checkDuplicate(3L, 9L));
        assertEquals(ErrorCode.ENROLL_DUPLICATE, error.getErrorCode());
    }

    @Test
    void noDuplicateCanContinue() {
        jdbc.duplicateCount = 0;
        assertDoesNotThrow(() -> rules.checkDuplicate(3L, 9L));
    }

    @Test
    void overlappingClassReturnsReadableConflict() {
        jdbc.rows = List.of(
                Map.of("course_name", "数据结构", "day_of_week", 1,
                        "start_section", 3, "end_section", 4));
        BizException error = assertThrows(BizException.class, () -> rules.checkTimeConflict(3L, 9L));
        assertEquals(ErrorCode.ENROLL_TIME_CONFLICT, error.getErrorCode());
        assertTrue(error.getMessage().contains("数据结构"));
        assertTrue(error.getMessage().contains("周一"));
    }

    @Test
    void missingPrerequisiteIsRejected() {
        jdbc.rows = List.of(Map.of("course_name", "程序设计"));
        BizException error = assertThrows(BizException.class, () -> rules.checkPrerequisite(3L, 12L));
        assertEquals(ErrorCode.ENROLL_PREREQ_FAILED, error.getErrorCode());
        assertTrue(error.getMessage().contains("程序设计"));
    }

    private static class StubJdbcTemplate extends JdbcTemplate {
        int duplicateCount;
        List<Map<String, Object>> rows = List.of();

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            return requiredType.cast(duplicateCount);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args) {
            return rows;
        }
    }
}
