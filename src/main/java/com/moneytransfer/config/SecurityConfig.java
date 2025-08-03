package com.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                // Allow access to health check endpoints without authentication
                .antMatchers("/ping", "/", "/actuator/health", "/actuator/info").permitAll()
                // Allow access to H2 console in development (disabled in production)
                .antMatchers("/h2-console/**").hasRole("ADMIN")
                // Require authentication for all other endpoints
                .anyRequest().authenticated()
            .and()
                .httpBasic()
            .and()
                .csrf().disable() // Disable CSRF for API endpoints
                .headers().frameOptions().disable(); // Allow H2 console frames
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
} 