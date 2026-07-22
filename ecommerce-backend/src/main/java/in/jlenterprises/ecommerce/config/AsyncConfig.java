package in.jlenterprises.ecommerce.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables {@code @Async} (email, WhatsApp campaign worker) with a BOUNDED executor.
 * Boot's default uses an unbounded queue — a campaign or email burst on the free-tier
 * dyno (512 MB) could balloon memory until the instance died. When the queue is full,
 * CallerRunsPolicy makes the submitting thread do the work itself: throughput degrades
 * gracefully instead of tasks being lost or memory growing without limit.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("jl-async-");
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(200);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // A void @Async method's exception otherwise vanishes silently.
        return (throwable, method, params) ->
                log.error("Async {} failed: {}", method.getName(), throwable.toString(), throwable);
    }
}
