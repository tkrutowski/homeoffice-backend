package net.focik.homeoffice.emailservice.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for email service
 * - Enables async task execution for email sending
 * - Enables scheduled task execution for periodic emails
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class EmailConfig {

    /**
     * Thread pool for asynchronous email sending
     * Prevents blocking of HTTP threads during mail operations
     */
    @Bean(name = "emailTaskExecutor")
    public TaskExecutor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("email-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Email task executor initialized with core pool size: 2, max pool size: 5");
        return executor;
    }
}
