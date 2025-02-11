package aicreative.ai.dataplane.proxy.huoshan.tts.async.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("dp-proxy-huoshanTtsAsyncTask")
@Data
public class HuoshanTtsAsyncTask {
    @Id
    private String taskId;
    @Indexed
    private String taskType;
    private String executionId;
    private Boolean useEmotion;
}