package com.moneytransfer.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if ("admin".equals(username)) {
            return new User("admin", 
                "$2a$10$7JdUICerD/gW9oY6GRBLBO7nPWopUKEnUPYlu9zKY0/8t/yahZgta", // admin123 encoded
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        }
        throw new UsernameNotFoundException("User not found: " + username);
    }
} 