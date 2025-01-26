package aicreative.ai.dataplane.task;

import aicreative.ai.dataplane.task.config.TaskExecStrategyConfig;
import aicreative.ai.dataplane.task.enums.TaskFailType;
import aicreative.ai.dataplane.task.enums.TaskType;
import aicreative.ai.dataplane.task.repository.TaskDO;
import aicreative.ai.dataplane.task.repository.TaskDORepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
public class TaskTimeoutChecker {
    @Resource
    private TaskLockHelper taskLockHelper;
    @Resource
    private TaskQueue taskQueue;
    @Resource
    private TaskHandler taskHandler;
    @Resource
    private TaskDORepository taskDORepository;
    @Resource
    private TaskExecStrategyConfig execStrategyConfig;

    @Scheduled(fixedDelay = 10 * 1000L, scheduler = "taskTimeoutCheckScheduler")
    public void taskTimeoutCheckSchedule() {
        final String randomValue = UUID.randomUUID().toString();
        final String lockName = "dp-task:lock:timeout-check";
        try {
            taskLockHelper.lock(lockName, randomValue);
            taskTimeoutCheck();
        } catch (Exception e) {
            log.warn("Lock task timeout check failed", e);
        } finally {
            taskLockHelper.unlock(lockName, randomValue);
        }
    }

    private void taskTimeoutCheck() {
        Arrays.stream(TaskType.values()).forEach(taskType -> {
            Set<String> execTaskIds = taskQueue.getAllExecTask(taskType);
            execTaskIds.forEach(taskId -> {
                Optional<TaskDO> taskOpt = taskDORepository.findById(taskId);
                if (taskOpt.isEmpty()) {
                    return;
                }
                TaskDO task = taskOpt.get();
                if (!isTimeout(task)) {
                    return;
                }
                taskHandler.processTaskResult(TaskHandler.TaskResult.fail(task.getTaskId(),
                        task.getTaskType(), TaskFailType.TIMEOUT));
            });
        });
    }

    private boolean isTimeout(TaskDO task) {
        long now = System.currentTimeMillis();
        long startTime;
        try {
            startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(task.getExecStartTime()).getTime();
        } catch (ParseException e) {
            log.info("Parse task start time failed,taskId:{}", task.getTaskId(), e);
            return false;
        }
        long timeoutConfig = execStrategyConfig.getStrategyConfig(task.getTaskType()).getTimeout();
        return timeoutConfig > 0 && now - startTime > timeoutConfig;
    }

    @Configuration
    public static class TaskTimeoutCheckSchedulerConfig {
        @Bean
        public ThreadPoolTaskScheduler taskTimeoutCheckScheduler() {
            ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
            taskScheduler.setPoolSize(10);
            taskScheduler.setThreadNamePrefix("task-timeout-check");
            return taskScheduler;
        }
    }
}
