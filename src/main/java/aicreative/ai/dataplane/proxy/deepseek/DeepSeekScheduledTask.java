package aicreative.ai.dataplane.proxy.deepseek;

import aicreative.ai.dataplane.proxy.schedule.ResultHandler;
import aicreative.ai.dataplane.task.TaskHandler;
import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@Data
@AllArgsConstructor
public class DeepSeekScheduledTask {

    private final TaskHandler taskHandler;
    private final ResultHandler resultHandler;
    private final DeepSeekHandler deepSeekHandler;

    private final List<String> taskTypes = List.of("DeepSeekWorkflow");

    @Scheduled(fixedDelay = 5000)
    public void triggerTask() {
        taskTypes.forEach(type -> {
            final TaskHandler.TaskInfo taskInfo = taskHandler.fetchTask(TaskType.valueOf(type));
            if (Objects.isNull(taskInfo)) {
                return;
            }
            doTask(type, taskInfo.getTaskId(), taskInfo.getParam());
        });
    }

    private void doTask(String type, String taskId, Object param) {
        try {
            log.info("Request DeepSeek, taskId:{}", taskId);
            Object result = deepSeekHandler.request(taskId, param);
            resultHandler.replySuccess(taskId, TaskType.fromName(type), result);
        } catch (Exception e) {
            log.warn("Request DeepSeek failed, taskId:{}, type:{}, param:{}", taskId, type, param, e);
            resultHandler.replyFailed(taskId, TaskType.fromName(type), e.getMessage());
        }
    }
}
