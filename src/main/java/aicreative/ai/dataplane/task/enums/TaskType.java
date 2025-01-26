package aicreative.ai.dataplane.task.enums;

import java.util.Objects;

public enum TaskType {

    CozeWorkflow;

    public static TaskType fromName(String name) {
        for (TaskType taskType : TaskType.values()) {
            if (Objects.equals(taskType.name(), name)) {
                return taskType;
            }
        }
        return null;
    }
}
