package com.agri.ecommerce.config;

import com.agri.ecommerce.security.JwtAuthenticationFilter;
import com.agri.ecommerce.security.RestAccessDeniedHandler;
import com.agri.ecommerce.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/api/health",
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/public/**",
                    "/api/payments/vnpay/return",
                    "/api/payments/vnpay/ipn",
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/error"
                ).permitAll()
                .requestMatchers("/api/admin/categories/**")
                    .hasAuthority("manage_categories")
                .requestMatchers("/api/admin/products/**")
                    .hasAuthority("manage_products")
                .requestMatchers("/api/admin/users/**", "/api/roles/**")
                    .hasAuthority("manage_users")
                .requestMatchers("/api/admin/orders/**")
                    .hasAuthority("manage_orders")
                .requestMatchers("/api/admin/coupons/**")
                    .hasAuthority("manage_coupons")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
