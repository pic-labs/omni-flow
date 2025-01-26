package aicreative.ai.controlplane.coordinator.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("cp-coord-reqeust")
@Data
@Builder
public class CpRequestDO {

    @Id
    private String taskId;

    private String kindId;

    private String functionName;

    private Object param;
}
