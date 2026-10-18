package com.csms.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    public JwtService(org.springframework.jdbc.core.JdbcTemplate jdbc) { this.jdbc=jdbc; }

    @Value("${csms.jwt.secret}")
    private String secret;

    @Value("${csms.jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Long id, String username, String role, Long deptId) {
        return generateToken(id, username, role, deptId, 0);
    }

    public String generateToken(Long id, String username, String role, Long deptId, int version) {
        return generateToken(id,username,role,deptId,null,version);
    }

    public long expirationSeconds() {
        var values=jdbc.queryForList("SELECT config_value FROM sys_config WHERE config_key='auth.jwt_expire_seconds'",String.class);
        if(!values.isEmpty()) try { long n=Long.parseLong(values.get(0));if(n>=60 && n<=86400) return n; } catch(NumberFormatException ignored) { }
        return expiration/1000;
    }

    public String generateToken(Long id,String username,String role,Long deptId,Long majorId,int version) {
        return Jwts.builder()
                .subject(id.toString())
                .id(java.util.UUID.randomUUID().toString())
                .claim("id", id)
                .claim("username", username)
                .claim("ver", version)
                .claim("role", role)
                .claim("deptId", deptId)
                .claim("majorId",majorId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationSeconds()*1000))
                .signWith(getSigningKey())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token) {
        var claims=getClaims(token);
        return claims.get("username",String.class)==null ? claims.getSubject() : claims.get("username",String.class);
    }

    public int getVersion(String token) { var value=getClaims(token).get("ver",Integer.class); return value==null ? 0 : value; }

    public Long getId(String token) {
        return getClaims(token).get("id", Long.class);
    }

    public String getRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Long getDeptId(String token) {
        return getClaims(token).get("deptId", Long.class);
    }
}
