package com.csms.enroll.infrastructure;

import com.csms.enroll.domain.CapacityGuard;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CapacityGuardImpl implements CapacityGuard {

    private final JdbcTemplate jdbcTemplate;

    public CapacityGuardImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryOccupy(Long teachingClassId, Long userId) {
        String sql = "UPDATE teaching_class SET enrolled_count = enrolled_count + 1 WHERE id = ? AND status = 'PUBLISHED' AND deleted = 0 AND enrolled_count < capacity";
        int updated = jdbcTemplate.update(sql, teachingClassId);
        return updated > 0;
    }
}
