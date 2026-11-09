package io.github.tomaszfranczak.batchprocessor.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;

import static org.assertj.core.api.Assertions.assertThat;

class ApiUserConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class, ValidationAutoConfiguration.class))
            .withUserConfiguration(ApiUserConfig.class);

    @Test
    void configuredUserCanAuthenticate() {
        contextRunner
                .withPropertyValues("app.security.user.name=api-client", "app.security.user.password=s3cret")
                .run(context -> {
                    // when
                    UserDetails user = context.getBean(UserDetailsService.class).loadUserByUsername("api-client");

                    // then
                    assertThat(PasswordEncoderFactories.createDelegatingPasswordEncoder()
                            .matches("s3cret", user.getPassword())).isTrue();
                });
    }

    @Test
    void applicationDoesNotStartWithoutPassword() {
        contextRunner
                .withPropertyValues("app.security.user.name=api-client")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("password");
                });
    }

    @Test
    void applicationDoesNotStartWithBlankUsername() {
        contextRunner
                .withPropertyValues("app.security.user.name= ", "app.security.user.password=s3cret")
                .run(context -> {
                    // then
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).rootCause().hasMessageContaining("name");
                });
    }
}
