package aicreative.ai.dataplane.task.validator;

import aicreative.ai.dataplane.task.enums.TaskType;
import aicreative.ai.dataplane.task.repository.TaskDO;

public interface TaskResultValidator {
    boolean isTaskResultValid(TaskDO task, Object result);

    TaskType getTaskType();
}
