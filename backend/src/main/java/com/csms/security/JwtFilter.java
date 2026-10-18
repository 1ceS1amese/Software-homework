package com.csms.security;

import com.csms.common.context.CurrentUserHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccessRegistry access;

    public JwtFilter(JwtService jwtService, UserAccessRegistry access) {
        this.jwtService = jwtService;
        this.access = access;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                var claims=jwtService.getClaims(authHeader.substring(7));
                Long id=claims.get("id",Long.class),deptId=claims.get("deptId",Long.class);
                String username=claims.get("username",String.class),role=claims.get("role",String.class);
                if(username==null) username=claims.getSubject();
                Integer version=claims.get("ver",Integer.class);
                if(access.permits(id,username,role,version==null?0:version)) {
                    var principal=new AuthPrincipal(id,username,null,role,deptId);
                    var authentication=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    CurrentUserHolder.set(new CurrentUserHolder.UserContext(id,username,role,deptId));
                }
            } catch(io.jsonwebtoken.ExpiredJwtException e) { request.setAttribute("csms.expired",true); }
              catch(io.jsonwebtoken.JwtException|IllegalArgumentException e) { /* invalid token becomes 401 */ }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.remove();
        }
    }
}
