package com.rbs.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 启用异步方法支持
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "taskExecutor") // 指定线程池名称
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 核心线程数：当提交的任务数超过核心线程数时，会将任务放入队列
        executor.setMaxPoolSize(10); // 最大线程数：当队列满了之后，会创建新的线程直到达到最大线程数
        executor.setQueueCapacity(25); // 队列容量：存储等待执行的任务
        executor.setThreadNamePrefix("EmailExecutor-"); // 线程名称前缀，方便调试
        executor.setWaitForTasksToCompleteOnShutdown(true); // 关闭时等待任务完成
        executor.setAwaitTerminationSeconds(60); // 等待任务完成的最大时间
        executor.initialize(); // 初始化线程池
        return executor;
    }
}