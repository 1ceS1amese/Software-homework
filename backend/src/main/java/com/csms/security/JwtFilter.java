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
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token) && access.permits(jwtService.getId(token), jwtService.getUsername(token), jwtService.getRole(token), jwtService.getVersion(token))) {
                Long id = jwtService.getId(token);
                String username = jwtService.getUsername(token);
                String role = jwtService.getRole(token);
                Long deptId = jwtService.getDeptId(token);

                AuthPrincipal principal = new AuthPrincipal(id, username, null, role, deptId);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                CurrentUserHolder.UserContext userContext = new CurrentUserHolder.UserContext(id, username, role, deptId);
                CurrentUserHolder.set(userContext);
            }
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserHolder.remove();
        }
    }
}
