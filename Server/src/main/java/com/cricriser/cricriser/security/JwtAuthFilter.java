package com.cricriser.cricriser.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final JwtBlacklistService jwtBlacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, JwtBlacklistService jwtBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.jwtBlacklistService = jwtBlacklistService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Allow preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String path = request.getRequestURI();

        // Public routes
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            { "error": "Authorization token missing" }
        """);
            return;
        }

        String token = authHeader.substring(7);

        // Blacklist check
        if (jwtBlacklistService.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            { "error": "You are logged out. Please login again." }
        """);
            return;
        }

        try {
            // 🔑 Parse JWT ONLY ONCE
            String email = jwtUtil.extractEmail(token);

            UsernamePasswordAuthenticationToken authToken
                    = new UsernamePasswordAuthenticationToken(email, null, List.of());

            authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);

        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("""
            { "error": "Session expired. Please login again." }
        """);
        }
    }

    //  PUBLIC URL HELPER
    private boolean isPublicPath(String path) {
        return path.startsWith("/api/admin/signup")
                || path.startsWith("/api/admin/login")
                || path.startsWith("/api/admin/verify-otp")
                || path.startsWith("/api/admin/forgot-password")
                || path.startsWith("/api/admin/verify-forgot-otp")
                || path.startsWith("/api/player/signup")
                || path.startsWith("/api/player/login")
                || path.startsWith("/api/player/forgot-password")
                || path.startsWith("/api/player/verify-otp")
                || path.startsWith("/api/player/verify-forgot-otp");
    }
}
