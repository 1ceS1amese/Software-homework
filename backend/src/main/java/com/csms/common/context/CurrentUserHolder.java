package com.csms.common.context;

public class CurrentUserHolder {
    private static final ThreadLocal<UserContext> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(UserContext userContext) {
        THREAD_LOCAL.set(userContext);
    }

    public static UserContext get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    public static class UserContext {
        private Long id;
        private String username;
        private String role;
        private Long deptId;

        public UserContext() {}

        public UserContext(Long id, String username, String role, Long deptId) {
            this.id = id;
            this.username = username;
            this.role = role;
            this.deptId = deptId;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
    }
}
