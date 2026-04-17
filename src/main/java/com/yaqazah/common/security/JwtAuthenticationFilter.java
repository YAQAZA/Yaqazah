package com.yaqazah.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String jwt = null;

        // 1. Check if the Authorization header contains a Bearer token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Could not extract username from token", e);
            }
        }

        // 2. If we have an email and the user isn't already authenticated in this context
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Extract roles directly from the token (No database hit required!)
            List<String> roles = jwtUtil.extractRoles(jwt);

            // Null-safety check: Prevents crashes if the token is completely missing the roles claim
            if (roles == null) {
                roles = java.util.Collections.emptyList();
            }

            // Normalize roles: Spring's hasRole() strictly requires the "ROLE_" prefix.
            // This ensures it exists regardless of how it was saved in the database.
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Create a lightweight UserDetails object using the data from the token
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    email, "", authorities);

            // 3. Validate the token and set the security context
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // This tells Spring Security: "This user is fully authenticated and has these roles."
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 4. Continue the filter chain
        filterChain.doFilter(request, response);
    }
}