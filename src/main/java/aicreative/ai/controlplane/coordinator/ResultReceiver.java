package aicreative.ai.controlplane.coordinator;

import aicreative.ai.dataplane.task.enums.TaskStatus;

public interface ResultReceiver {

    void updateTask(String taskId, TaskStatus status, String message, Object result);

}
