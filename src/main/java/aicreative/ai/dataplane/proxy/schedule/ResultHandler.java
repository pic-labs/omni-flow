package aicreative.ai.dataplane.proxy.schedule;

import aicreative.ai.dataplane.task.TaskHandler;
import aicreative.ai.dataplane.task.enums.TaskFailType;
import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Data
public class ResultHandler {
    private final TaskHandler taskHandler;

    public ResultHandler(TaskHandler taskHandler) {
        this.taskHandler = taskHandler;
    }

    public void replySuccess(String taskId, TaskType taskType, Object result) {
        TaskHandler.TaskResult taskResult = new TaskHandler.TaskResult();
        taskResult.setTaskId(taskId);
        taskResult.setTaskType(taskType);
        taskResult.setResult(result);
        taskResult.setSuccess(true);
        taskHandler.processTaskResult(taskResult);
    }

    public void replyFailed(String taskId, TaskType taskType, String errorMsg) {
        TaskHandler.TaskResult taskResult = new TaskHandler.TaskResult();
        taskResult.setSuccess(false);
        taskResult.setTaskId(taskId);
        taskResult.setFailType(TaskFailType.INVOKE_FAIL);
        taskResult.setTaskType(taskType);
        taskResult.setResult(StringUtils.defaultIfBlank(errorMsg, "request failed"));
        taskHandler.processTaskResult(taskResult);
    }
}
