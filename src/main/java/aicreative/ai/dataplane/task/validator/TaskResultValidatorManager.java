package aicreative.ai.dataplane.task.validator;

import aicreative.ai.dataplane.task.enums.TaskType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TaskResultValidatorManager {
    @Resource
    private ApplicationContext ctx;
    private static final Map<TaskType, TaskResultValidator> VALIDATOR_MAP = new HashMap<>();

    @PostConstruct
    public void init() {
        ctx.getBeansOfType(TaskResultValidator.class).values()
                .forEach(validator -> VALIDATOR_MAP.put(validator.getTaskType(), validator));
    }

    public TaskResultValidator getValidator(TaskType taskType) {
        return VALIDATOR_MAP.get(taskType);
    }
}
