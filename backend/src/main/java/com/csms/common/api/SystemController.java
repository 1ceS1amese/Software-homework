package com.csms.common.api;

import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import static com.csms.common.service.CommandSupport.require;

@RestController
public class SystemController {
    private final HealthEndpoint health;private final JdbcTemplate jdbc;
    public SystemController(HealthEndpoint health,JdbcTemplate jdbc) { this.health=health;this.jdbc=jdbc; }
    @GetMapping("/api/v1/actuator/health")
    public ResponseEntity<?> health() {
        String status=health.health().getStatus().getCode();
        return ResponseEntity.status("UP".equals(status)?200:503).body(ApiResponse.success(Map.of("status",status)));
    }
    @GetMapping("/enrollment/reconcile")
    public ApiResponse<?> reconcile() {
        require("ADMIN");
        return ApiResponse.success(jdbc.queryForList("""
            SELECT tc.id AS teachingClassId,tc.enrolled_count AS enrolledCount,
              (SELECT COUNT(*) FROM enrollment e WHERE e.teaching_class_id=tc.id AND e.deleted=0 AND e.status IN ('ENROLLED','COMPLETED')) AS actualCount
            FROM teaching_class tc WHERE tc.deleted=0 HAVING enrolledCount<>actualCount
            """));
    }
}
