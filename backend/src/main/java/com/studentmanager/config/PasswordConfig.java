package com.studentmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Cost factor 12: deliberately slow, which is what you want for
        // password hashing. Never used for anything performance-sensitive.
        return new BCryptPasswordEncoder(12);
    }
}
