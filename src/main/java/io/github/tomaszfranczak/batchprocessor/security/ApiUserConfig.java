package io.github.tomaszfranczak.batchprocessor.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableConfigurationProperties(ApiUserProperties.class)
class ApiUserConfig {

    @Bean
    UserDetailsService userDetailsService(ApiUserProperties user) {
        return new InMemoryUserDetailsManager(User.withUsername(user.name())
                .password("{noop}" + user.password())
                .build());
    }
}
