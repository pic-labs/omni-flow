package aicreative.ai.dataplane.proxy.coze.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("dp-proxy-cozetask")
@Data
public class CozeTask {
    @Id
    private String taskId;
    @Indexed
    private String taskType;
    private String workflowId;
    private String executionId;
}