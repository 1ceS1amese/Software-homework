package com.csms.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.ConcurrentHashMap;

/** AS-07：单体内缓存五分钟，账户变更在提交后立即失效；不缓存密码或令牌。 */
@Component
public class UserAccessRegistry {
    private record Access(String status,String role,String username,int version,long expires) {}
    private final ConcurrentHashMap<Long,Access> cache=new ConcurrentHashMap<>();
    private final JdbcTemplate jdbc;
    public UserAccessRegistry(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public boolean permits(Long id,String username,String role,int version) {
        if(id==null || username==null || role==null) return false;
        var access=cache.compute(id,(key,old)-> {
            long now=System.nanoTime();
            if(old!=null && old.expires()>now) return old;
            var rows=jdbc.queryForList("SELECT status,user_type,username,version FROM sys_user WHERE id=? AND deleted=0",id);
            if(rows.isEmpty()) return new Access("DELETED","","",-1,now+300_000_000_000L);
            var row=rows.get(0);
            return new Access(String.valueOf(row.get("status")),String.valueOf(row.get("user_type")),String.valueOf(row.get("username")),((Number)row.get("version")).intValue(),now+300_000_000_000L);
        });
        return "ACTIVE".equals(access.status()) && access.version()==version && access.username().equals(username) && access.role().equals(role);
    }
    public void changed(Long id) {
        if(TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { cache.remove(id); }
            });
        } else cache.remove(id);
    }
}
