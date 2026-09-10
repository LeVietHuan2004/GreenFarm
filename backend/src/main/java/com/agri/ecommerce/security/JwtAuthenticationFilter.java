package com.agri.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
        JwtService jwtService,
        CustomUserDetailsService userDetailsService,
        RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authorization.substring(BEARER_PREFIX.length());
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (!jwtService.isValid(token, userDetails)
                || !userDetails.isEnabled()
                || !userDetails.isAccountNonLocked()) {
                throw new BadCredentialsException("Token khong hop le");
            }

            UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("Token khong hop le", exception)
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
