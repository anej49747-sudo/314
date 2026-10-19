package csit314.renovation.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ManagerBootstrapConfiguration {
    @Bean
    @Profile("local")
    @ConditionalOnProperty(prefix = "app.manager.bootstrap", name = "enabled", havingValue = "true")
    ApplicationRunner initialPlatformManager(ManagerBootstrapService bootstrap,
            @Value("${MANAGER_BOOTSTRAP_EMAIL:}") String email,
            @Value("${MANAGER_BOOTSTRAP_PASSWORD:}") String password) {
        return arguments -> bootstrap.ensureCreated(email, password);
    }
}
