package ge.dola.talanti.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor; // STRICT IMPORT

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncSecurityConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(5);
        delegate.setMaxPoolSize(10);
        delegate.setThreadNamePrefix("TalantiAsync-");
        delegate.initialize();

        // Natively compatible with Spring's @Async while maintaining Context Propagation
        return new DelegatingSecurityContextAsyncTaskExecutor(delegate);
    }
}