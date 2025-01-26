package aicreative.ai.dataplane.task.config;

import aicreative.ai.dataplane.task.enums.TaskType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;

@Data
@Configuration
@ConfigurationProperties(prefix = "task.exec-strategy")
public class TaskExecStrategyConfig {
    private Map<TaskType, StrategyConfig> taskTypeStrategyMap;

    public StrategyConfig getStrategyConfig(TaskType taskType) {
        StrategyConfig config = taskTypeStrategyMap.get(taskType);
        if (Objects.isNull(config)) {
            return new StrategyConfig();
        }
        return config;
    }

    @Data
    public static class StrategyConfig {
        private Integer retryCnt = -1;
        private Long timeout = -1L;
    }
}
