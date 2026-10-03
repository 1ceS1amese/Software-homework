package com.csms.common.error;

public enum ErrorCode {
    SUCCESS(0, "成功"),
    
    // 4.1 通用错误码
    PARAM_ERROR(10000, "参数校验失败"),
    BIZ_ERROR(10001, "业务规则拒绝"),
    FORBIDDEN(10002, "无权限"),
    NOT_FOUND(10003, "资源不存在"),
    CONFLICT(10004, "资源冲突"),
    SYSTEM_BUSY(10005, "系统繁忙"),
    SYSTEM_ERROR(10006, "服务器内部错误"),

    // 4.2 认证错误码
    AUTH_BAD_CREDENTIALS(20001, "用户名或密码错误"),
    AUTH_LOCKED(20002, "账号已锁定，请N分钟后再试"),
    AUTH_DISABLED(20003, "账号已禁用"),
    AUTH_BAD_OLD_PASSWORD(20004, "旧密码错误"),
    AUTH_EXPIRED(20005, "登录已过期，请重新登录"),
    AUTH_INVALID_TOKEN(20006, "登录凭证无效"),

    // 4.3 基础数据错误码
    DEPT_CODE_EXISTS(30101, "院系代码已存在"),
    DEPT_HAS_RECORDS(30102, "院系存在下级专业或用户，无法删除"),
    MAJOR_CODE_EXISTS(30201, "专业代码已存在"),
    TERM_CODE_EXISTS(30301, "学期代码已存在"),
    COURSE_CODE_EXISTS(30401, "课程代码已存在"),
    COURSE_PREREQ_CYCLE(30501, "先修关系将形成循环依赖"),
    SYS_CONFIG_READONLY(30601, "系统参数不可修改"),

    // 4.4 教学班错误码
    TC_EXISTS(40101, "同课程同学期已存在同名教学班"),
    TC_CAPACITY_INVALID(40102, "容量不能小于当前已选人数"),
    TC_STATUS_INVALID(40103, "当前状态不允许该操作"),
    TC_SCHEDULE_CONFLICT(40104, "上课时间段互相重叠"),
    TC_HAS_ENROLLMENTS(40105, "存在选课记录，不允许删除"),

    // 4.5 选课错误码
    ENROLL_DUPLICATE(50101, "你已经选择了该教学班"),
    ENROLL_FULL(50102, "名额已满，未能选课"),
    ENROLL_TIME_CONFLICT(50103, "与上课时间冲突"),
    ENROLL_PREREQ_FAILED(50104, "需先修读并通过课程"),
    ENROLL_OUT_OF_TIME(50105, "当前不在选课时间"),
    ENROLL_NOT_OPEN(50106, "该教学班未开放选课"),
    ENROLL_WITHDRAW_DEADLINE(50107, "已过退课截止时间"),
    ENROLL_ALREADY_STARTED(50108, "教学班已开课，不能退课"),
    ENROLL_CREDIT_MAX(50109, "本学期选课学分超过上限"),
    ENROLL_CREDIT_MIN(50110, "本学期选课学分低于下限，不能退课"),
    ENROLL_ACCOUNT_ABNORMAL(50111, "账户状态异常，无法选课"),
    ENROLL_SYSTEM_BUSY(50112, "选课人数众多，请稍后重试"),

    // 4.6 成绩错误码
    GRADE_INVALID(60101, "成绩必须在0~100之间"),
    GRADE_INCOMPLETE(60102, "存在未录入成绩的学生，无法发布"),
    GRADE_PUBLISHED(60103, "成绩已发布，如需修改请联系管理员解锁"),
    GRADE_UNLOCK_FORBIDDEN(60104, "只有管理员可以解锁已发布成绩"),
    GRADE_UNLOCK_REASON_REQUIRED(60105, "解锁必须填写原因"),
    GRADE_IMPORT_INVALID(60106, "导入文件中存在非法数据"),

    // 4.7 用户错误码
    USER_EXISTS(70101, "用户名已存在"),
    USER_EXISTS_DELETED(70102, "该用户名已存在但已注销"),
    USER_BUILTIN(70103, "内置数据不允许删除"),
    USER_HAS_ENROLLMENTS(70104, "用户已存在选课记录，不能删除");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
