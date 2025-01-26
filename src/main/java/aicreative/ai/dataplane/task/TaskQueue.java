package aicreative.ai.dataplane.task;

import aicreative.ai.dataplane.task.enums.TaskType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TaskQueue {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String PENDING_TASK_QUEUE_PREFIX = "dp-task:queue:pending:";
    private static final String EXEC_TASK_QUEUE_PREFIX = "dp-task:queue:exec:";


    public void addPendingTask(String taskId, TaskType taskType, Long priority) {
        stringRedisTemplate.opsForZSet().add(getPendingTaskQueueKey(taskType), taskId, priority);
    }

    public String getPendingTask(TaskType taskType) {
        Set<String> values = stringRedisTemplate.opsForZSet().range(getPendingTaskQueueKey(taskType), 0, 0);
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.iterator().next();
    }

    public void removePendingTask(String taskId, TaskType taskType) {
        stringRedisTemplate.opsForZSet().remove(getPendingTaskQueueKey(taskType), taskId);
    }

    public void addExecTask(String taskId, TaskType taskType) {
        stringRedisTemplate.opsForHash().put(getExecTaskHashKey(taskType), taskId, "1");
    }

    public void removeExecTask(String taskId, TaskType taskType) {
        stringRedisTemplate.opsForHash().delete(getExecTaskHashKey(taskType), taskId);
    }

    public boolean isInExecQueue(TaskType taskType, String taskId) {
        return stringRedisTemplate.opsForHash().hasKey(getExecTaskHashKey(taskType), taskId);
    }

    public Set<String> getAllExecTask(TaskType taskType) {
        return stringRedisTemplate.opsForHash().keys(getExecTaskHashKey(taskType)).stream()
                .map(String::valueOf).collect(Collectors.toSet());
    }

    private String getExecTaskHashKey(TaskType taskType) {
        return EXEC_TASK_QUEUE_PREFIX + taskType.name();
    }

    private String getPendingTaskQueueKey(TaskType taskType) {
        return PENDING_TASK_QUEUE_PREFIX + taskType.name();
    }
}
