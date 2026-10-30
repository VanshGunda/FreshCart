package com.freshcart.config;

import com.freshcart.services.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final String allowedOrigin;
    private final RequestRateLimitFilter requestRateLimitFilter;

    public SecurityConfig(JwtFilter jwtFilter, CustomUserDetailsService userDetailsService,
                          @Value("${app.cors.allowed-origins:${app.cors.allowed-origin:http://localhost:5173}}") String allowedOrigin,
                          RequestRateLimitFilter requestRateLimitFilter) {
        this.jwtFilter = jwtFilter;
        this.userDetailsService = userDetailsService;
        this.allowedOrigin = allowedOrigin;
        this.requestRateLimitFilter = requestRateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless bearer-token API: CSRF protection is not applicable while credentials are only sent in Authorization headers.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentTypeOptions(content -> {})
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(false).maxAgeInSeconds(31536000)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Authentication required\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Access denied\"}");
                }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers("/auth/reg", "/auth/verify-otp", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/product/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/cart/**", "/order/**").hasRole("USER")
                .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/product/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(requestRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtFilter, RequestRateLimitFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(allowedOrigin.split(","))
                .map(String::trim).filter(v -> !v.isBlank()).toList();
        if (origins.contains("*")) {
            throw new IllegalStateException("Wildcard CORS origin is not allowed");
        }
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "X-Requested-With"));
        // The application uses bearer tokens rather than cookies, so credentials are not required.
        configuration.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}
