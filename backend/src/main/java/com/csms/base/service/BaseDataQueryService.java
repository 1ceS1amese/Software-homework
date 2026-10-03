package com.csms.base.service;

import com.csms.common.error.BizException;
import com.csms.common.error.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基础数据只读查询（B-01 / B-05 / B-09 / B-13 / B-21）。
 */
@Service
public class BaseDataQueryService {

    private final JdbcTemplate jdbc;

    public BaseDataQueryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // —— 院系 ——
    public List<Map<String, Object>> depts(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT id AS id, code AS code, name AS name, leader AS leader,
                       status AS status, sort_no AS sortNo
                  FROM dept WHERE deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (name LIKE ? OR code LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY sort_no, id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // —— 专业 ——
    public List<Map<String, Object>> majors(Long deptId, String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT m.id AS id, m.dept_id AS deptId, d.name AS deptName,
                       m.code AS code, m.name AS name,
                       m.degree_years AS degreeYears, m.status AS status
                  FROM major m
                  LEFT JOIN dept d ON d.id = m.dept_id
                 WHERE m.deleted = 0
                """);
        List<Object> args = new ArrayList<>();
        if (deptId != null) {
            sql.append(" AND m.dept_id = ?");
            args.add(deptId);
        }
        if (StringUtils.hasText(keyword)) {
            sql.append(" AND (m.name LIKE ? OR m.code LIKE ?)");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        sql.append(" ORDER BY m.dept_id, m.id");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // —— 学期 ——
    public List<Map<String, Object>> terms() {
        return jdbc.queryForList("""
                SELECT id AS id, code AS code, name AS name,
                       DATE_FORMAT(start_date, '%Y-%m-%d')    AS startDate,
                       DATE_FORMAT(end_date, '%Y-%m-%d')      AS endDate,
                       DATE_FORMAT(enroll_start, '%Y-%m-%d %H:%i:%s')  AS enrollStart,
                       DATE_FORMAT(enroll_end, '%Y-%m-%d %H:%i:%s')    AS enrollEnd,
                       DATE_FORMAT(withdraw_end, '%Y-%m-%d %H:%i:%s')  AS withdrawEnd,
                       status AS status
                  FROM term WHERE deleted = 0
                 ORDER BY start_date DESC
                """);
    }

    /** 当前学期：优先 ENROLLING，其次最新 */
    public Map<String, Object> currentTerm() {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id AS id, code AS code, name AS name,
                       DATE_FORMAT(start_date, '%Y-%m-%d')    AS startDate,
                       DATE_FORMAT(end_date, '%Y-%m-%d')      AS endDate,
                       DATE_FORMAT(enroll_start, '%Y-%m-%d %H:%i:%s')  AS enrollStart,
                       DATE_FORMAT(enroll_end, '%Y-%m-%d %H:%i:%s')    AS enrollEnd,
                       DATE_FORMAT(withdraw_end, '%Y-%m-%d %H:%i:%s')  AS withdrawEnd,
                       status AS status
                  FROM term WHERE deleted = 0
                 ORDER BY (status = 'ENROLLING') DESC, start_date DESC
                 LIMIT 1
                """);
        return rows.isEmpty() ? null : rows.get(0);
    }

    // —— 课程 ——
    public List<Map<String, Object>> courses(Long deptId, Long majorId, String courseType,
                                             String status, String keyword, Integer page, Integer size) {
        int p = (page == null || page < 1) ? 1 : page;
        int s = (size == null || size < 1) ? 20 : Math.min(size, 200);

        StringBuilder where = new StringBuilder(" WHERE c.deleted = 0 ");
        List<Object> args = new ArrayList<>();
        if (deptId != null) {
            where.append(" AND c.dept_id = ? ");
            args.add(deptId);
        }
        if (majorId != null) {
            where.append(" AND c.major_id = ? ");
            args.add(majorId);
        }
        if (StringUtils.hasText(courseType)) {
            where.append(" AND c.course_type = ? ");
            args.add(courseType);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND c.status = ? ");
            args.add(status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (c.name LIKE ? OR c.course_code LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }

        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(s);
        pageArgs.add((p - 1) * s);

        return jdbc.queryForList("""
                SELECT c.id AS id, c.course_code AS courseCode, c.name AS name,
                       c.credit AS credit, c.credit_hours AS creditHours,
                       c.course_type AS courseType,
                       c.dept_id AS deptId, d.name AS deptName,
                       c.major_id AS majorId, m.name AS majorName,
                       c.description AS description, c.status AS status
                  FROM course c
                  LEFT JOIN dept d  ON d.id = c.dept_id
                  LEFT JOIN major m ON m.id = c.major_id
                """ + where + " ORDER BY c.course_code LIMIT ? OFFSET ?", pageArgs.toArray());
    }

    public long courseCount(Long deptId, Long majorId, String courseType, String status, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE c.deleted = 0 ");
        List<Object> args = new ArrayList<>();
        if (deptId != null) {
            where.append(" AND c.dept_id = ? ");
            args.add(deptId);
        }
        if (majorId != null) {
            where.append(" AND c.major_id = ? ");
            args.add(majorId);
        }
        if (StringUtils.hasText(courseType)) {
            where.append(" AND c.course_type = ? ");
            args.add(courseType);
        }
        if (StringUtils.hasText(status)) {
            where.append(" AND c.status = ? ");
            args.add(status);
        }
        if (StringUtils.hasText(keyword)) {
            where.append(" AND (c.name LIKE ? OR c.course_code LIKE ?) ");
            args.add("%" + keyword + "%");
            args.add("%" + keyword + "%");
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM course c " + where, Long.class, args.toArray());
        return total == null ? 0 : total;
    }

    // —— 系统参数 ——
    public List<Map<String, Object>> configs() {
        return jdbc.queryForList("""
                SELECT id AS id, config_key AS configKey, config_value AS configValue,
                       value_type AS valueType, description AS description, editable AS editable
                  FROM sys_config
                 ORDER BY id
                """);
    }

    /** 修改系统参数（仅 editable=1 的项可改） */
    public void updateConfig(Long id, String value) {
        List<Integer> editable = jdbc.query("SELECT editable FROM sys_config WHERE id = ?",
                (rs, rowNum) -> rs.getInt(1), id);
        if (editable.isEmpty()) {
            throw new BizException(ErrorCode.NOT_FOUND, "系统参数不存在");
        }
        if (editable.get(0) == 0) {
            throw new BizException(ErrorCode.SYS_CONFIG_READONLY);
        }
        jdbc.update("UPDATE sys_config SET config_value = ? WHERE id = ?", value, id);
    }
}
