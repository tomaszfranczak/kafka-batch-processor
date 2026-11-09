package io.github.tomaszfranczak.batchprocessor.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.security.user")
record ApiUserProperties(@NotBlank String name, @NotBlank String password) {
}
