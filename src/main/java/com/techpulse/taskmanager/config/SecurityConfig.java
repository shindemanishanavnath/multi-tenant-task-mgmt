package com.techpulse.taskmanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techpulse.taskmanager.security.JwtAuthenticationFilter;
import com.techpulse.taskmanager.security.JwtService;
import com.techpulse.taskmanager.security.RestAccessDeniedHandler;
import com.techpulse.taskmanager.security.RestAuthenticationEntryPoint;
import com.techpulse.taskmanager.security.TenantParamGuardFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtService jwtService,
                                                   ObjectMapper objectMapper,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // Stateless JWT API: no session, no CSRF token, no form/basic login.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // The H2 console renders in frames.
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(antMatcher(POST, "/tenant/register"), antMatcher(POST, "/auth/login")).permitAll()
                        .requestMatchers(antMatcher("/h2-console/**"), antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher(POST, "/users")).hasRole("ADMIN")
                        .requestMatchers(antMatcher(GET, "/users")).hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(antMatcher(POST, "/tasks")).hasRole("MANAGER")
                        .requestMatchers(antMatcher(PUT, "/tasks/*/assign")).hasRole("MANAGER")
                        .requestMatchers(antMatcher(PUT, "/tasks/*/status")).hasRole("USER")
                        .requestMatchers(antMatcher(GET, "/tasks")).hasAnyRole("USER", "MANAGER", "ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new TenantParamGuardFilter(objectMapper), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
